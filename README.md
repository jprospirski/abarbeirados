# Abarbeirados

Sistema de gestão de uma barbearia desenvolvido como projeto acadêmico do 4º período de Engenharia de Software / Análise e Desenvolvimento de Sistemas da Uniamérica.

A aplicação reúne uma API REST em Spring Boot e uma interface web em Angular para administrar clientes, serviços, barbeiros e agendamentos.

## Funcionalidades

- Login e proteção das rotas da aplicação.
- Cadastro, consulta, edição, busca e exclusão de clientes.
- Cadastro e gerenciamento de serviços, com preço, duração e situação ativo/inativo.
- Cadastro de barbeiros e associação dos serviços realizados por cada profissional.
- Criação e edição de agendamentos com cliente, barbeiro, serviço, data, horário e observações.
- Valor e duração do serviço preservados no histórico do agendamento.
- Agenda diária com busca, filtro por data e diferentes formas de visualização.
- Controle dos estados `AGENDADO`, `CONFIRMADO`, `CONCLUIDO` e `CANCELADO`.
- Tratamento padronizado dos erros devolvidos pela API.
- Documentação interativa da API com Swagger UI.

## Tecnologias

### Back-end

- Java 17
- Spring Boot 4.1
- Spring Web MVC
- Spring Data JPA
- Bean Validation
- PostgreSQL
- Flyway
- Lombok
- springdoc-openapi / Swagger UI
- JUnit 5, Mockito, MockMvc e JaCoCo

### Front-end

- Angular 19.2
- TypeScript 5.7
- Angular standalone components e signals
- Reactive Forms
- Bootstrap 5 e MDB Angular UI Kit
- SweetAlert2
- Jasmine e Karma

## Estrutura do projeto

```text
abarbeirados/
├── back/                 API Spring Boot
│   └── src/
│       ├── main/
│       │   ├── java/     controllers, services, repositories, DTOs e entidades
│       │   └── resources/ configurações e migrations do Flyway
│       └── test/         testes de controllers, services, mappers e repositories
└── front/                aplicação Angular
    └── src/app/
        ├── core/         models, services, guards e utilitários
        ├── features/     telas organizadas por funcionalidade
        └── shared/       componentes compartilhados
```

## Pré-requisitos

- JDK 17 com a variável `JAVA_HOME` configurada.
- PostgreSQL em execução.
- Node.js e npm.

## Configuração do banco

Crie o banco de dados:

```sql
CREATE DATABASE abarbeirados;
```

Por padrão, o backend utiliza:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/abarbeirados
spring.datasource.username=postgres
spring.datasource.password=postgres
```

Se necessário, ajuste esses valores em `back/src/main/resources/application.properties`.

O Flyway cria e versiona o schema automaticamente. O Hibernate usa `ddl-auto=validate`, portanto a aplicação interrompe a inicialização se as entidades não corresponderem ao banco.

## Executando o backend

No Windows:

```powershell
cd back
.\mvnw.cmd spring-boot:run
```

No Linux ou macOS:

```bash
cd back
./mvnw spring-boot:run
```

O backend fica disponível em `http://localhost:8080`.

## Executando o frontend

Em outro terminal:

```bash
cd front
npm ci
npm start
```

Abra `http://localhost:4200`. Durante o desenvolvimento, o proxy do Angular encaminha as chamadas `/api` para o backend na porta 8080.

### Acesso

A autenticação atual é local e simulada no frontend:

- Usuário: `admin`
- Senha: `123456`

## API

| Recurso | Caminho base | Operações principais |
|---|---|---|
| Clientes | `/api/clientes` | criar, listar, buscar, atualizar e excluir |
| Serviços | `/api/servicos` | criar, listar, filtrar, buscar, atualizar e excluir |
| Barbeiros | `/api/barbeiros` | criar, listar, filtrar, buscar, consultar serviços, atualizar e excluir |
| Agendamentos | `/api/agendamentos` | criar, listar, consultar agenda, buscar, atualizar, alterar status e excluir |

Com o backend iniciado:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Especificação OpenAPI: `http://localhost:8080/v3/api-docs`

## Testes e qualidade

Execute os testes do backend:

```powershell
cd back
.\mvnw.cmd test
```

Para executar também a verificação de cobertura:

```powershell
.\mvnw.cmd clean verify
```

Os testes de contexto e repository utilizam o PostgreSQL configurado na aplicação. O relatório do JaCoCo é gerado em `back/target/site/jacoco/index.html`, e o build exige cobertura mínima global de 90%.

Para testar ou compilar o frontend:

```bash
cd front
npm test
npm run build
```

## Equipe

| Integrante | Papel | Branch |
|---|---|---|
| João Pedro Rospirski Pegorini | Product Owner | `JP` |
| Cauã Buch Domingues | Scrum Master | `domingues` |
| Christopher Adam Oliveira dos Santos | Desenvolvedor | `Alok` |
| Leonardo Barth | Desenvolvedor | `Leo` |

---

Projeto acadêmico — Abarbeirados — Uniamérica, 2026.
