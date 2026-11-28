# Abarbeirados

Sistema de gestão de agendamentos para barbearia — projeto acadêmico do 4º período de
Engenharia de Software / Análise e Desenvolvimento de Sistemas da **Uniamerica**,
atendendo a uma demanda real encaminhada à faculdade.

API REST em Spring Boot com Cliente, Serviço, Barbeiro e Agendamento; frontend em
Angular consumindo a API, com autenticação, agenda com controle de conflito de
horário e telas de cadastro completas. Roda ponta a ponta, com banco PostgreSQL
versionado por Flyway.

## Equipe

| Integrante | Papel | Branch |
|---|---|---|
| João Pedro Rospirski Pegorini | Product Owner | `JP` |
| Cauã Buch Domingues | Scrum Master | `domingues` |
| Christopher Adam Oliveira dos Santos | Desenvolvedor | `Alok` |
| Leonardo Barth | Desenvolvedor | `Leo` |

## Stack

**Back-end** — Java 17 · Spring Boot 4.1.0 (Web MVC, Data JPA, Bean Validation,
OpenFeign) · Lombok · Flyway · PostgreSQL · springdoc-openapi (Swagger UI) · SLF4J

**Front-end** — Angular 19.2 (standalone components, signals, `@if`/`@for`) ·
TypeScript 5.7 · MDB-Angular UI Kit + Bootstrap 5 · SweetAlert2

## Funcionalidades

- Login com controle de acesso por rota
- CRUD de clientes, com busca por nome e preenchimento de endereço por CEP
  (integração com a API ViaCEP via OpenFeign)
- CRUD de serviços, com valor, duração, situação ativo/inativo e combinações
- CRUD de barbeiros, com os serviços que cada um executa
- Agendamentos com cliente, serviço, barbeiro, data/hora e observações; valor e
  duração congelados do serviço no momento da marcação
- Grade de disponibilidade de horários, sinalizando o motivo de cada
  indisponibilidade (horário já passado, conflito, tempo insuficiente até o
  fechamento)
- Controle de status do agendamento (Agendado → Confirmado → Concluído / Cancelado)
- Painel da agenda do dia, com filtro por data e busca

## Estrutura

```
abarbeirados/
├── back/    Spring Boot — controller / service / repository / dto / model / mapper
└── front/   Angular — core (models, services, guards) / features / shared
```

## Como executar

O backend precisa subir antes do frontend. A senha do PostgreSQL não fica no
código: informe-a na variável de ambiente `DB_PASSWORD` antes de subir o backend
ou rodar os testes.

```bash
export DB_PASSWORD=sua_senha        # Linux / macOS
$env:DB_PASSWORD = "sua_senha"      # Windows (PowerShell)
```

```bash
cd back
./mvnw spring-boot:run      # Linux / macOS
mvnw.cmd spring-boot:run    # Windows
```

```bash
cd front
npm install
npm start
```

Backend em `http://localhost:8080`, frontend em `http://localhost:4200`. O
`proxy.conf.json` encaminha `/api` para a 8080, então não há CORS em
desenvolvimento.

### Login

A tela inicial exige login com autenticação JWT. Não há usuário padrão: crie o
primeiro em **Criar usuário** (`/cadastro`) e depois entre com ele. A senha é
gravada em BCrypt e o token expira em 1 hora.

### Variáveis de ambiente

Nenhuma credencial fica no código. `DB_PASSWORD` é sempre obrigatória; as demais
têm um padrão de desenvolvimento e devem ser definidas em produção.

| Variável | Para que serve | Padrão em desenvolvimento |
|---|---|---|
| `DB_URL` | URL JDBC do PostgreSQL | `jdbc:postgresql://localhost:5432/abarbeirados` |
| `DB_USERNAME` | usuário do banco | `postgres` |
| `DB_PASSWORD` | senha do banco | sem padrão (obrigatória) |
| `JWT_SECRET` | chave de assinatura do token, em Base64 (`openssl rand -base64 48`) | chave aleatória a cada subida |
| `CORS_ALLOWED_ORIGINS` | origens liberadas no CORS, separadas por vírgula | `http://localhost:4200` |

### Documentação da API

Com o backend no ar, a documentação interativa fica em
`http://localhost:8080/swagger-ui.html`, com todos os endpoints prontos para
execução direto pelo navegador (ou importáveis no Insomnia/Postman a partir de
`http://localhost:8080/v3/api-docs`).

### Banco de dados

**PostgreSQL** — crie o banco `abarbeirados`. Usuário, senha e endereço vêm das
variáveis de ambiente `DB_USERNAME`, `DB_PASSWORD` e `DB_URL` (padrão de
desenvolvimento: usuário `postgres` em `localhost:5432`; a senha não tem padrão).

As migrations do Flyway (`back/src/main/resources/db/migration`) criam o schema e
populam o catálogo de serviços automaticamente na inicialização do backend.

## Deploy na AWS (EC2)

O script `provisionar-ec2.sh`, na raiz do repositório, prepara uma instância do
zero: instala JDK 17, Node.js 22, PostgreSQL e Apache, cria o banco, gera as
credenciais, faz o build do back-end e do front-end e deixa tudo no ar.

**1. Crie a instância**

- AMI **Ubuntu Server 26.04 LTS** (ou 24.04), tipo `t3.small` ou maior, disco de
  8 GB ou mais.
- **Security Group** com apenas duas regras de entrada:

  | Porta | Origem | Para quê |
  |---|---|---|
  | 22 (SSH) | somente o seu IP | administração |
  | 80 (HTTP) | `0.0.0.0/0` | aplicação |

  As portas 8080 (back-end) e 5432 (PostgreSQL) **não** são liberadas: os dois
  escutam só em `127.0.0.1`, e quem atende a internet é o Apache.

**2. Execute o script**

```bash
ssh -i sua-chave.pem ubuntu@IP_PUBLICO
git clone https://github.com/jprospirski/abarbeirados.git
cd abarbeirados
sudo bash provisionar-ec2.sh
```

Leva alguns minutos. Ao final o script mostra o endereço da aplicação. Acesse
`http://IP_PUBLICO/cadastro` para criar o primeiro usuário e depois faça login.

O script deve ser executado de dentro da pasta do projeto, onde ficam `back/` e
`front/`. Para atualizar a aplicação depois, rode `git pull` nessa pasta e
execute o script de novo: ele mantém as credenciais já geradas, refaz o build e
reinicia o serviço.

**O que o script configura**

| Item | Onde fica |
|---|---|
| Back-end (serviço systemd `abarbeirados`) | `/opt/abarbeirados/abarbeirados.jar` |
| Front-end (build de produção do Angular) | `/var/www/abarbeirados` |
| Apache (fallback de rotas SPA e proxy de `/api`) | `/etc/apache2/sites-available/abarbeirados.conf` |
| Credenciais (senha do banco e chave do JWT, geradas na instância) | `/etc/abarbeirados.env`, legível só pelo root |

**Logs e operação**

```bash
sudo journalctl -u abarbeirados -f                 # log do back-end em tempo real
sudo journalctl -u abarbeirados -n 100 --no-pager  # últimas 100 linhas
sudo systemctl status abarbeirados                 # situação do serviço
sudo systemctl restart abarbeirados                # reiniciar o back-end
sudo tail -f /var/log/apache2/access.log /var/log/apache2/error.log
```

---

Projeto acadêmico — Abarbeirados — Uniamerica, 2026.