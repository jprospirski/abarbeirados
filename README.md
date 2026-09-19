# Abarbeirados

Sistema de gestão de agendamentos para barbearia — projeto acadêmico do 4º período de
Engenharia de Software / Análise e Desenvolvimento de Sistemas da **Uniamerica**,
atendendo a uma demanda real encaminhada à faculdade.

API REST em Spring Boot com Cliente, Serviço, Barbeiro e Agendamento; frontend em
Angular consumindo a API, com autenticação, agenda com controle de conflito de
horário e telas de cadastro completas. Roda ponta a ponta, com banco H2 em memória
versionado por Flyway.

## Equipe

| Integrante | Papel | Branch |
|---|---|---|
| João Pedro Rospirski Pegorini | Product Owner | `jp` |
| Cauã Buch Domingues | Scrum Master | `domingues` |
| Christopher Adam Oliveira dos Santos | Desenvolvedor | `Alok` |
| Leonardo Barth | Desenvolvedor | `Leo` |

## Stack

**Back-end** — Java 17 · Spring Boot 4.1.0 (Web MVC, Data JPA, Bean Validation,
OpenFeign) · Lombok · Flyway · H2 em memória · springdoc-openapi (Swagger UI) · SLF4J

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

O backend precisa subir antes do frontend.

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

A tela inicial exige login (mockado nesta fase, sem back-end de autenticação):
usuário `admin`, senha `123456`.

### Documentação da API

Com o backend no ar, a documentação interativa fica em
`http://localhost:8080/swagger-ui.html`, com todos os endpoints prontos para
execução direto pelo navegador (ou importáveis no Insomnia/Postman a partir de
`http://localhost:8080/v3/api-docs`).

### Banco de dados

**Console do H2** — `http://localhost:8080/h2-console`, JDBC URL
`jdbc:h2:mem:abarbeirados`, usuário `sa`, senha em branco.

O banco é em memória e nasce vazio a cada reinicialização. Não é preciso cadastrar
nada à mão: as migrations do Flyway (`back/src/main/resources/db/migration`)
recriam o schema e populam o catálogo de serviços automaticamente na inicialização
do backend.

---

Projeto acadêmico — Abarbeirados — Uniamerica, 2026.