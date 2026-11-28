#!/bin/bash
#
# Abarbeirados - prepara uma instancia EC2 (Ubuntu) e publica a aplicacao.
#
# Uso, de dentro da pasta do projeto (onde ficam back/ e front/):
#   sudo bash provisionar-ec2.sh
#
# Security Group da instancia: porta 22 (so o seu IP) e porta 80 (publica).
# As portas 8080 (back-end) e 5432 (PostgreSQL) NAO devem ser liberadas.

set -e
cd "$(dirname "$0")"
PROJETO="$(pwd)"

# ---------- 1. swap ----------
# instancia pequena (1 GB de memoria) nao aguenta o build do Angular sem swap
if [ ! -f /swapfile ]; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

# ---------- 2. pacotes ----------
apt update
apt install -y openjdk-17-jdk postgresql postgresql-contrib apache2 curl

# o Angular 19 precisa de um Node mais novo que o do repositorio do Ubuntu
curl -fsSL https://deb.nodesource.com/setup_22.x | bash -
apt install -y nodejs

java -version
node -v
timedatectl set-timezone America/Sao_Paulo

# ---------- 3. senhas ----------
# a senha do banco e a chave do JWT sao geradas aqui, na instancia, e ficam
# num arquivo que so o root le. Nada disso vai para o Git.
if [ ! -f /etc/abarbeirados.env ]; then
  IP_PUBLICO="$(curl -s https://checkip.amazonaws.com)"
  cat > /etc/abarbeirados.env <<EOF
DB_URL=jdbc:postgresql://127.0.0.1:5432/abarbeirados
DB_USERNAME=abarbeirados
DB_PASSWORD=$(openssl rand -hex 16)
JWT_SECRET=$(openssl rand -base64 48)
CORS_ALLOWED_ORIGINS=http://${IP_PUBLICO}
SERVER_ADDRESS=127.0.0.1
EOF
  chmod 600 /etc/abarbeirados.env
fi
. /etc/abarbeirados.env

# ---------- 4. banco de dados ----------
# as tabelas sao criadas pelo Flyway quando o back-end sobe
systemctl enable --now postgresql
sudo -u postgres psql -c "CREATE USER abarbeirados WITH PASSWORD '${DB_PASSWORD}';" || echo "usuario ja existe"
sudo -u postgres psql -c "CREATE DATABASE abarbeirados OWNER abarbeirados;" || echo "banco ja existe"

# ---------- 5. build do back-end ----------
cd "${PROJETO}/back"
sed -i 's/\r$//' mvnw     # tira quebra de linha do Windows, se houver
chmod +x mvnw
./mvnw -DskipTests clean package
mkdir -p /opt/abarbeirados
cp target/*.jar /opt/abarbeirados/abarbeirados.jar

# ---------- 6. build do front-end ----------
cd "${PROJETO}/front"
export NG_CLI_ANALYTICS=false
export NODE_OPTIONS=--max-old-space-size=1536
npm ci
npx ng build --configuration production
rm -rf /var/www/abarbeirados
mkdir -p /var/www/abarbeirados
cp -r dist/front/browser/* /var/www/abarbeirados/

# ---------- 7. back-end como servico ----------
# o systemd sobe o back-end no boot e reinicia se ele cair
cat > /etc/systemd/system/abarbeirados.service <<EOF
[Unit]
Description=Abarbeirados - API Spring Boot
After=network.target postgresql.service

[Service]
User=www-data
EnvironmentFile=/etc/abarbeirados.env
ExecStart=/usr/bin/java -Xmx400m -jar /opt/abarbeirados/abarbeirados.jar
Restart=always

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable abarbeirados
systemctl restart abarbeirados

# ---------- 8. apache ----------
# o Apache entrega o Angular e repassa /api para o back-end.
# FallbackResource devolve o index.html nas rotas do Angular (/agendamentos,
# /clientes...), para a pagina nao dar 404 ao ser recarregada.
a2enmod proxy proxy_http
cat > /etc/apache2/sites-available/abarbeirados.conf <<EOF
<VirtualHost *:80>
    DocumentRoot /var/www/abarbeirados

    ProxyPreserveHost On
    ProxyPass /api http://127.0.0.1:8080/api
    ProxyPass /v3/api-docs http://127.0.0.1:8080/v3/api-docs
    ProxyPass /swagger-ui.html http://127.0.0.1:8080/swagger-ui.html
    ProxyPass /swagger-ui http://127.0.0.1:8080/swagger-ui

    <Directory /var/www/abarbeirados>
        Require all granted
        FallbackResource /index.html
    </Directory>
</VirtualHost>
EOF

a2dissite 000-default
a2ensite abarbeirados
systemctl restart apache2

# ---------- 9. verificacao ----------
# sem token a API responde 401: sinal de que o back-end subiu, conectou no
# banco e esta com a seguranca ativa
echo "Aguardando o back-end subir..."
for i in $(seq 1 60); do
  CODIGO="$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/api/servicos || true)"
  if [ "${CODIGO}" = "401" ]; then
    break
  fi
  sleep 2
done

if [ "${CODIGO}" != "401" ]; then
  echo "ERRO: o back-end nao respondeu (HTTP ${CODIGO}). Veja o log:"
  journalctl -u abarbeirados -n 40 --no-pager
  exit 1
fi

echo "============================================================"
echo " Abarbeirados publicado"
echo " Aplicacao : ${CORS_ALLOWED_ORIGINS}"
echo " Cadastro  : ${CORS_ALLOWED_ORIGINS}/cadastro"
echo " Logs      : sudo journalctl -u abarbeirados -f"
echo "============================================================"
