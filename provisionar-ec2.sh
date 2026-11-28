#!/usr/bin/env bash
#
# Abarbeirados - provisionamento completo de uma instancia EC2 (Ubuntu 22.04/24.04).
#
# Instala e configura tudo o que a aplicacao precisa, sem intervencao manual:
#   - JDK 17, Node.js 20, PostgreSQL, Nginx e Git
#   - banco e usuario do PostgreSQL, com senha gerada na hora
#   - build do back-end (Spring Boot) e do front-end (Angular)
#   - back-end como servico systemd (sobe sozinho no boot e reinicia se cair)
#   - Nginx servindo o Angular, com fallback de rotas SPA, e repassando /api ao back-end
#
# Uso (na instancia, como usuario ubuntu):
#   sudo bash provisionar-ec2.sh
#
# Pode ser executado mais de uma vez: reaproveita as senhas ja geradas e apenas
# atualiza o codigo, refaz o build e reinicia o servico.
#
# Variaveis opcionais (ex.: sudo BRANCH=developer bash provisionar-ec2.sh):
#   REPO_URL  repositorio a clonar   (padrao: https://github.com/jprospirski/abarbeirados.git)
#   BRANCH    branch a publicar      (padrao: main)
#   DOMINIO   dominio da aplicacao   (padrao: IP publico da instancia)
#
# Security Group esperado na instancia: 22 (SSH, so o seu IP) e 80 (HTTP, publico).
# As portas 8080 (back-end) e 5432 (PostgreSQL) NAO devem ser liberadas: os dois
# so escutam em 127.0.0.1 e quem atende a internet e o Nginx.

set -euo pipefail

REPO_URL="${REPO_URL:-https://github.com/jprospirski/abarbeirados.git}"
BRANCH="${BRANCH:-main}"
DOMINIO="${DOMINIO:-}"

APP_NOME="abarbeirados"
APP_DIR="/opt/${APP_NOME}"
SRC_DIR="${APP_DIR}/src"
JAR_DESTINO="${APP_DIR}/app/${APP_NOME}.jar"
WEB_DIR="/var/www/${APP_NOME}"
ENV_DIR="/etc/${APP_NOME}"
ENV_ARQUIVO="${ENV_DIR}/${APP_NOME}.env"
DB_NOME="${APP_NOME}"
DB_USUARIO="${APP_NOME}"
FUSO="America/Sao_Paulo"

etapa() { printf '\n\033[1;36m==> %s\033[0m\n' "$*"; }
falha() { printf '\n\033[1;31mERRO: %s\033[0m\n' "$*" >&2; exit 1; }

trap 'falha "o script parou na linha ${LINENO}. Veja a mensagem acima."' ERR

# ---------------------------------------------------------------- pre-requisitos

[ "$(id -u)" -eq 0 ] || falha "execute com sudo: sudo bash $0"

. /etc/os-release
case "${ID}" in
  ubuntu|debian) ;;
  *) falha "este script foi feito para Ubuntu/Debian (apt). Sistema encontrado: ${ID}." ;;
esac

export DEBIAN_FRONTEND=noninteractive

# ---------------------------------------------------------------- memoria (swap)

# - instancias pequenas (t2/t3.micro, 1 GB) nao tem memoria para o build do
#   Angular e do Maven; um swap de 2 GB evita que o build seja morto no meio.
etapa "Verificando memoria swap"
if [ "$(swapon --show --noheadings | wc -l)" -eq 0 ]; then
  if [ ! -f /swapfile ]; then
    fallocate -l 2G /swapfile || dd if=/dev/zero of=/swapfile bs=1M count=2048 status=none
    chmod 600 /swapfile
    mkswap /swapfile >/dev/null
  fi
  swapon /swapfile
  grep -q '^/swapfile ' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
  echo "Swap de 2 GB ativado."
else
  echo "Swap ja existe."
fi

# ---------------------------------------------------------------- pacotes

etapa "Instalando pacotes do sistema (JDK 17, PostgreSQL, Nginx, Git)"
apt-get update -y -q
apt-get install -y -q openjdk-17-jdk-headless postgresql nginx git curl openssl rsync ca-certificates

# - o Angular 19 exige Node 20+; o do repositorio padrao do Ubuntu e mais antigo
etapa "Instalando Node.js 20"
NODE_MAJOR="$(node -v 2>/dev/null | sed -E 's/^v([0-9]+).*/\1/' || true)"
if [ -z "${NODE_MAJOR}" ] || [ "${NODE_MAJOR}" -lt 20 ]; then
  curl -fsSL https://deb.nodesource.com/setup_20.x | bash - >/dev/null
  apt-get install -y -q nodejs
fi
echo "Node $(node -v) / npm $(npm -v) / $(java -version 2>&1 | head -n 1)"

timedatectl set-timezone "${FUSO}" || true

# ---------------------------------------------------------------- codigo-fonte

# - se o script estiver dentro de uma copia do projeto (pastas back/ e front/ ao
#   lado dele), publica essa copia; senao, clona o repositorio do GitHub.
etapa "Obtendo o codigo-fonte"
SCRIPT_DIR=""
if [ -n "${BASH_SOURCE[0]:-}" ] && [ -f "${BASH_SOURCE[0]}" ]; then
  SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
fi

mkdir -p "${APP_DIR}/app"

if [ -n "${SCRIPT_DIR}" ] && [ -f "${SCRIPT_DIR}/back/pom.xml" ] && [ -f "${SCRIPT_DIR}/front/package.json" ] \
   && [ "${SCRIPT_DIR}" != "${SRC_DIR}" ]; then
  echo "Usando a copia local em ${SCRIPT_DIR}"
  mkdir -p "${SRC_DIR}"
  rsync -a --delete \
    --exclude '.git' --exclude 'node_modules' --exclude 'target' --exclude 'dist' --exclude '.angular' \
    "${SCRIPT_DIR}/" "${SRC_DIR}/"
elif [ -d "${SRC_DIR}/.git" ]; then
  echo "Atualizando o clone existente (${BRANCH})"
  git -C "${SRC_DIR}" fetch --quiet origin "${BRANCH}"
  git -C "${SRC_DIR}" checkout --quiet -B "${BRANCH}" "origin/${BRANCH}"
else
  echo "Clonando ${REPO_URL} (${BRANCH})"
  rm -rf "${SRC_DIR}"
  git clone --quiet --depth 1 --branch "${BRANCH}" "${REPO_URL}" "${SRC_DIR}" \
    || falha "nao foi possivel clonar ${REPO_URL}. Se o repositorio for privado, copie o projeto para a instancia e rode o script de dentro dele."
fi

[ -f "${SRC_DIR}/back/pom.xml" ] || falha "codigo-fonte incompleto em ${SRC_DIR} (falta back/pom.xml)."

# ---------------------------------------------------------------- segredos

# - senha do banco e chave do JWT sao geradas aqui, na instancia, e nunca vao
#   para o Git. Ficam num arquivo que so o root le; o systemd as entrega ao back-end.
etapa "Gerando credenciais (arquivo ${ENV_ARQUIVO})"

if [ -z "${DOMINIO}" ]; then
  TOKEN_IMDS="$(curl -fsS -m 3 -X PUT 'http://169.254.169.254/latest/api/token' \
    -H 'X-aws-ec2-metadata-token-ttl-seconds: 60' 2>/dev/null || true)"
  DOMINIO="$(curl -fsS -m 3 -H "X-aws-ec2-metadata-token: ${TOKEN_IMDS}" \
    'http://169.254.169.254/latest/meta-data/public-ipv4' 2>/dev/null || true)"
fi
[ -n "${DOMINIO}" ] || DOMINIO="$(curl -fsS -m 5 https://checkip.amazonaws.com 2>/dev/null | tr -d '[:space:]' || true)"
[ -n "${DOMINIO}" ] || DOMINIO="localhost"

DB_SENHA=""
JWT_CHAVE=""
if [ -f "${ENV_ARQUIVO}" ]; then
  DB_SENHA="$(grep -E '^DB_PASSWORD=' "${ENV_ARQUIVO}" | cut -d= -f2- || true)"
  JWT_CHAVE="$(grep -E '^JWT_SECRET=' "${ENV_ARQUIVO}" | cut -d= -f2- || true)"
  echo "Reaproveitando as credenciais ja existentes."
fi
[ -n "${DB_SENHA}" ] || DB_SENHA="$(openssl rand -hex 24)"
[ -n "${JWT_CHAVE}" ] || JWT_CHAVE="$(openssl rand -base64 48 | tr -d '\n')"

mkdir -p "${ENV_DIR}"
umask 077
cat > "${ENV_ARQUIVO}" <<EOF
DB_URL=jdbc:postgresql://127.0.0.1:5432/${DB_NOME}
DB_USERNAME=${DB_USUARIO}
DB_PASSWORD=${DB_SENHA}
JWT_SECRET=${JWT_CHAVE}
CORS_ALLOWED_ORIGINS=http://${DOMINIO},https://${DOMINIO}
SERVER_ADDRESS=127.0.0.1
SERVER_PORT=8080
EOF
umask 022
chown root:root "${ENV_ARQUIVO}"
chmod 600 "${ENV_ARQUIVO}"

# ---------------------------------------------------------------- banco de dados

# - o PostgreSQL do Ubuntu ja escuta so em localhost; nada aqui abre a 5432 para fora.
#   As tabelas sao criadas pelo Flyway quando o back-end sobe.
etapa "Configurando o PostgreSQL (banco e usuario ${DB_NOME})"
systemctl enable --now postgresql >/dev/null

sudo -u postgres psql -v ON_ERROR_STOP=1 -q <<EOF
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${DB_USUARIO}') THEN
    CREATE ROLE ${DB_USUARIO} LOGIN PASSWORD '${DB_SENHA}';
  ELSE
    ALTER ROLE ${DB_USUARIO} LOGIN PASSWORD '${DB_SENHA}';
  END IF;
END
\$\$;
EOF

if ! sudo -u postgres psql -tAc "SELECT 1 FROM pg_database WHERE datname = '${DB_NOME}'" | grep -q 1; then
  sudo -u postgres createdb --owner="${DB_USUARIO}" --encoding=UTF8 --template=template0 "${DB_NOME}"
  echo "Banco ${DB_NOME} criado."
else
  echo "Banco ${DB_NOME} ja existe."
fi

# ---------------------------------------------------------------- build do back-end

etapa "Compilando o back-end (Spring Boot)"
cd "${SRC_DIR}/back"
sed -i 's/\r$//' mvnw          # - protege contra quebra de linha do Windows
chmod +x mvnw
export MAVEN_OPTS="-Xmx512m"
./mvnw -q -B -DskipTests clean package

JAR_GERADO="$(find target -maxdepth 1 -name '*.jar' ! -name '*.original' | head -n 1)"
[ -n "${JAR_GERADO}" ] || falha "o build do back-end nao gerou o .jar."

# ---------------------------------------------------------------- build do front-end

etapa "Compilando o front-end (Angular, build de producao)"
cd "${SRC_DIR}/front"
export NODE_OPTIONS="--max-old-space-size=1536"
npm ci --no-audit --no-fund
npx ng build --configuration production

DIST="${SRC_DIR}/front/dist/front/browser"
[ -f "${DIST}/index.html" ] || falha "o build do Angular nao gerou ${DIST}/index.html."

# ---------------------------------------------------------------- publicacao

etapa "Publicando os arquivos"
id -u "${APP_NOME}" >/dev/null 2>&1 || useradd --system --no-create-home --shell /usr/sbin/nologin "${APP_NOME}"

systemctl stop "${APP_NOME}" 2>/dev/null || true
install -o "${APP_NOME}" -g "${APP_NOME}" -m 0640 "${SRC_DIR}/back/${JAR_GERADO}" "${JAR_DESTINO}"
chown "${APP_NOME}:${APP_NOME}" "${APP_DIR}/app"

mkdir -p "${WEB_DIR}"
rsync -a --delete "${DIST}/" "${WEB_DIR}/"
chown -R www-data:www-data "${WEB_DIR}"

# ---------------------------------------------------------------- servico systemd

etapa "Registrando o back-end como servico systemd"
cat > "/etc/systemd/system/${APP_NOME}.service" <<EOF
[Unit]
Description=Abarbeirados - API Spring Boot
After=network.target postgresql.service
Wants=postgresql.service

[Service]
Type=simple
User=${APP_NOME}
Group=${APP_NOME}
EnvironmentFile=${ENV_ARQUIVO}
WorkingDirectory=${APP_DIR}/app
ExecStart=/usr/bin/java -Xms128m -Xmx400m -Duser.timezone=${FUSO} -jar ${JAR_DESTINO}
SuccessExitStatus=143
Restart=always
RestartSec=5
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=full

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable "${APP_NOME}" >/dev/null
systemctl restart "${APP_NOME}"

# ---------------------------------------------------------------- nginx

# - try_files devolve o index.html para qualquer rota que nao seja arquivo: e o
#   que faz /agendamentos, /clientes etc. funcionarem ao recarregar a pagina (SPA).
etapa "Configurando o Nginx"
cat > "/etc/nginx/sites-available/${APP_NOME}" <<'EOF'
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    server_name _;

    root /var/www/abarbeirados;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # documentacao da API (Swagger)
    location ~ ^/(swagger-ui|v3/api-docs) {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
EOF

ln -sf "/etc/nginx/sites-available/${APP_NOME}" "/etc/nginx/sites-enabled/${APP_NOME}"
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl enable nginx >/dev/null
systemctl reload nginx || systemctl restart nginx

# ---------------------------------------------------------------- verificacao

# - sem token a API protegida responde 401: e o sinal de que o back-end subiu,
#   conectou no banco e esta com a seguranca ativa.
etapa "Aguardando o back-end responder"
CODIGO=""
for _ in $(seq 1 60); do
  CODIGO="$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/api/servicos || true)"
  [ "${CODIGO}" = "401" ] && break
  sleep 2
done

if [ "${CODIGO}" != "401" ]; then
  journalctl -u "${APP_NOME}" -n 40 --no-pager || true
  falha "o back-end nao respondeu como esperado (HTTP ${CODIGO:-sem resposta}). Veja o log acima."
fi

CODIGO_WEB="$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1/agendamentos || true)"
[ "${CODIGO_WEB}" = "200" ] || falha "o Nginx nao entregou o front-end (HTTP ${CODIGO_WEB:-sem resposta})."

trap - ERR

cat <<EOF

============================================================
 Abarbeirados publicado com sucesso
============================================================
 Aplicacao : http://${DOMINIO}
 Cadastro  : http://${DOMINIO}/cadastro   (crie o primeiro usuario aqui)
 Swagger   : http://${DOMINIO}/swagger-ui.html

 Logs do back-end   : sudo journalctl -u ${APP_NOME} -f
 Ultimas 100 linhas : sudo journalctl -u ${APP_NOME} -n 100 --no-pager
 Status do servico  : sudo systemctl status ${APP_NOME}
 Reiniciar          : sudo systemctl restart ${APP_NOME}
 Logs do Nginx      : sudo tail -f /var/log/nginx/access.log /var/log/nginx/error.log

 Portas em escuta (8080 e 5432 devem aparecer so em 127.0.0.1):
$(ss -ltnH | awk '{print "   " $4}' | grep -E ':(22|80|8080|5432)$' | sort -u)

 Security Group: libere apenas 22 (seu IP) e 80 (publico).
============================================================
EOF
