# Abarbeirados

Sistema de gestão para barbearia — projeto acadêmico do 4º período de Engenharia de
Software / Análise e Desenvolvimento de Sistemas da **Uniamerica**, atendendo a uma
demanda real encaminhada à faculdade.

Backend Spring Boot com CRUD de Cliente, Serviço, Barbeiro e Agendamento; frontend
Angular consumindo a API. Roda ponta a ponta, com H2 em memória.

## Equipe

| Integrante | Branch |
|---|---|
| João Pedro Rospirski Pegorini | `jp` |
| Cauã Buch Domingues | `domingues` |
| Christopher Adam | `Alok` |
| Leonardo Barth | `Leo` |

## Stack

Java 17 · Spring Boot 4.1.0 (Web MVC + Data JPA) · Lombok · H2 em memória ·
Angular 19.2 · TypeScript 5.7 · Node.js 20.11+

---

## Entrega Parcial 1 — Aplicação com CRUD completo

| # | Critério | Peso | Status |
|---|---|:---:|:---:|
| 1 | Projeto Spring Boot com pacotes ajustados ao projeto (não usar `demo`) | 2 | ✅ |
| 2 | CRUD completo | 4 | ✅ |
| 3 | Mínimo de 6 endpoints — verbos HTTP adequados | 1 | ✅ |
| 4 | Mínimo de 6 endpoints — códigos HTTP adequados | 1 | ✅ |
| 5 | Mínimo de 6 endpoints — retorno estruturado | 1 | ✅ |
| 6 | Ao menos um `@PathVariable`, um `@RequestParam` e um `@RequestBody` | 1 | ✅ |
| 7 | Estruturação MVC adequada ao projeto | 3 | ✅ |
| 8 | Uso de Lombok e `record` | 2 | ✅ |
| 9 | Uso de DTOs | 2 | ✅ |
| | **Total** | **17** | |

---

## API

Base: `http://localhost:8080`

Documentação interativa em **`/swagger-ui/index.html`** — os quatro controllers
(`Cliente`, `Servico`, `Barbeiro`, `Agendamento`) com os DTOs de request e response,
e os endpoints executáveis direto da página.

`GET /api/clientes/cep/{cep}` consulta a ViaCEP por Feign, e o formulário de cliente
usa a resposta para conferir o endereço — o CEP não é gravado na tabela `clientes`.

---

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
`proxy.conf.json` encaminha `/api` para a 8080, então não há CORS em desenvolvimento.

O terminal exige `JAVA_HOME` apontando para o JDK 17; pelo IntelliJ, basta definir o
SDK do projeto e rodar `AbarbeiradosApplication`.

**Console do H2** — `http://localhost:8080/h2-console`, JDBC URL
`jdbc:h2:mem:abarbeirados`, usuário `sa`, senha em branco.

**Primeiro uso** — o banco é em memória e nasce vazio, perdendo os dados a cada
reinicialização. Não é preciso cadastrar nada à mão: o **Flyway** é o dono do schema
e roda as migrations de `back/src/main/resources/db/migration` a cada boot —
`V1__schema_inicial.sql` cria as tabelas e `V2__seed_servicos.sql` insere os 8
serviços (itens avulsos e combinações) que o formulário de marcação espera. O
Hibernate fica em `ddl-auto=validate` e só confere que as entidades batem com o que
as migrations criaram.

Barbeiro não vem semeado: cadastre ao menos um em **Barbeiros** antes de marcar o
primeiro agendamento, porque o campo é obrigatório no formulário.

---

Projeto acadêmico — Uniamerica, 2026.
