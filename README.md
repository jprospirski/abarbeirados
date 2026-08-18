# Abarbeirados

Sistema de gestão para barbearia — projeto acadêmico do 4º período de Engenharia de
Software / Análise e Desenvolvimento de Sistemas da **Uniamerica**, atendendo a uma
demanda real encaminhada à faculdade.

Backend Spring Boot com CRUD de Cliente, Serviço e Agendamento; frontend Angular
consumindo a API. Roda ponta a ponta, com H2 em memória.

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

**1 · Pacotes** — base `uniamerica.abarbeirados`. Nenhuma ocorrência de
`com.example` ou `demo`.

**2 · CRUD completo** — `Cliente`, `Servico` e `Agendamento`, cada um com o ciclo
inteiro (criar, listar, buscar, atualizar, excluir) e com model, repository,
service, mapper e DTOs próprios.

**3 · Verbos** — 17 endpoints: `GET` (7), `POST` (3), `PUT` (3), `PATCH` (1) e
`DELETE` (3). O `PATCH` é o avanço de status do agendamento, que altera um campo
só e por isso não é um `PUT`.

**4 · Códigos HTTP** — `201` na criação, `200` em leitura e atualização, `204` na
exclusão, `400` em validação, `404` em recurso ou rota inexistente e `409` ao
excluir registro em uso.

**5 · Retorno estruturado** — nenhum endpoint devolve entidade JPA; tudo passa por
DTO de resposta. Erros seguem o formato único `ApiError`, montado no
`GlobalException`.

**6 · Parâmetros** — `@PathVariable` nas rotas `/{id}` (10 usos), `@RequestParam`
nos filtros `nome`, `busca`, `data` e `apenasAtivos` (5 usos) e `@RequestBody` nos
`POST`, `PUT` e `PATCH` (7 usos).

**7 · MVC** — oito camadas: `controller`, `service`, `repository`, `mapper`,
`dto`, `model`, `exception` e `config`. Nenhum controller importa entidade JPA — só
DTOs. Regra de negócio e acesso ao repositório ficam no service; a conversão
entidade ↔ DTO, nos mappers `@Component`.

**8 · Lombok e `record`** — Lombok em 9 arquivos (`@Getter`, `@Setter`,
`@NoArgsConstructor`, `@AllArgsConstructor` e `@Builder` nas entidades;
`@RequiredArgsConstructor` em controllers e services). Os 9 DTOs são `record`.

**9 · DTOs** — request e response separados por operação, agrupados por domínio em
`dto/cliente`, `dto/servico`, `dto/agendamento` e `dto/error`. A entrada carrega a
validação; a saída expõe só o necessário.

**Entregar:** link do repositório no GitHub e ZIP com o `src`.

---

## API

Base: `http://localhost:8080`

| Verbo | Rota | Retorno |
|---|---|---|
| `POST` | `/api/clientes` | `201` · `400` |
| `GET` | `/api/clientes?nome=` | `200` |
| `GET` | `/api/clientes/{id}` | `200` · `404` |
| `PUT` | `/api/clientes/{id}` | `200` · `404` |
| `DELETE` | `/api/clientes/{id}` | `204` · `404` · `409` |
| `POST` | `/api/servicos` | `201` · `400` |
| `GET` | `/api/servicos?nome=&apenasAtivos=` | `200` |
| `GET` | `/api/servicos/{id}` | `200` · `404` |
| `PUT` | `/api/servicos/{id}` | `200` · `404` |
| `DELETE` | `/api/servicos/{id}` | `204` · `404` · `409` |
| `POST` | `/api/agendamentos` | `201` · `400` · `404` |
| `GET` | `/api/agendamentos?busca=&data=` | `200` |
| `GET` | `/api/agendamentos/agenda` | `200` |
| `GET` | `/api/agendamentos/{id}` | `200` · `404` |
| `PUT` | `/api/agendamentos/{id}` | `200` · `404` |
| `PATCH` | `/api/agendamentos/{id}/status` | `200` · `404` |
| `DELETE` | `/api/agendamentos/{id}` | `204` · `404` |

O agendamento referencia `Cliente` e `Servico` por chave estrangeira, mas guarda
`valor` e `duracaoMinutos` copiados do serviço no momento da marcação: se o preço
do catálogo mudar depois, o histórico preserva o que foi cobrado.

Frontend: `/agendamentos` é a listagem (três visualizações, filtro por dia e troca
de status) e `/agendamentos/novo` é o formulário de marcação.

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
reinicialização. Cadastre um serviço antes de abrir o formulário de marcação:

```bash
curl -X POST http://localhost:8080/api/servicos \
  -H "Content-Type: application/json" \
  -d '{"nome":"Corte masculino","valor":45.00,"duracaoMinutos":30}'
```

---

## Próximos passos

Especificação do demandante · migração para PostgreSQL 18 · carga inicial do
catálogo · validação de conflito de horário · telas de Cliente e Serviço ·
testes automatizados.

---

Projeto acadêmico — Uniamerica, 2026.
