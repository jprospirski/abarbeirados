# Abarbeirados

Sistema de gestão para barbearia, desenvolvido como projeto acadêmico do 4º período
do curso de Engenharia de Software / Análise e Desenvolvimento de Sistemas da Uniamerica.

O projeto nasce de uma demanda real encaminhada à faculdade. O documento de
especificação do demandante ainda não foi formalizado, portanto o escopo funcional
detalhado permanece em aberto — o desenvolvimento segue com base no domínio
(barbearia) e nos requisitos técnicos já definidos para a primeira entrega.

---

## Status

**Em desenvolvimento — primeira entrega.**

| Frente | Situação |
|---|---|
| Backend (Spring Boot) | Estrutura inicial criada |
| Frontend (Angular) | Não iniciado |
| Banco de dados | H2 em memória (provisório) — migração para PostgreSQL 18 planejada |
| Especificação do demandante | Pendente |

---

## Escopo da primeira entrega

Requisitos técnicos definidos pela faculdade, independentes da especificação
funcional do demandante:

- [ ] **1 CRUD completo** — criação, leitura, atualização e remoção de uma entidade do domínio
- [ ] **6 endpoints funcionais** na API REST
- [ ] **2 telas funcionais** no frontend

> As entidades e regras de negócio serão definidas quando o documento do demandante
> for formalizado. Até lá, o time desenvolve sobre a estrutura acordada, mantendo o
> código preparado para acomodar o escopo final sem retrabalho estrutural.

---

## Equipe

| Integrante |
|---|
| João Pedro Rospirski Pegorini |
| Cauã Buch Domingues |
| Christopher Adam |
| Leonardo Barth |

**Instituição:** Uniamerica
**Curso:** Engenharia de Software / Análise e Desenvolvimento de Sistemas
**Período:** 4º

---

## Tecnologias

### Backend

| Tecnologia | Versão | Observação |
|---|---|---|
| Java | 17 | Baseline exigido pelo Spring Boot 4.x |
| Spring Boot | 4.1.0 | Web MVC, Data JPA |
| Maven | 3.9.16 | Via Maven Wrapper (`mvnw`), não requer instalação |
| Lombok | — | Gerenciado pelo Spring Boot |
| H2 Database | — | Provisório, em memória, para desenvolvimento inicial |
| PostgreSQL | 18 | Banco definitivo — integração ainda não realizada |

### Frontend

| Tecnologia | Versão | Observação |
|---|---|---|
| Angular | 19 | Ainda não inicializado no repositório |
| Node.js | 20.11+ | Ver nota de compatibilidade abaixo |

> **Nota de compatibilidade — Node.js**
> O Angular 19 exige Node.js `^18.19.1`, `^20.11.1` ou `^22.x`. Versões anteriores,
> incluindo a linha 14, não são suportadas e falham já na instalação do Angular CLI.
> Recomenda-se a versão **20.11 LTS ou superior** para todo o time do frontend.

### Ferramentas de apoio

- **DBeaver** — cliente para inspeção e administração do PostgreSQL
- **IntelliJ IDEA** — IDE padrão do time de backend

---

## Estrutura do repositório

```
abarbeirados/
├── .mvn/wrapper/            # Maven Wrapper
├── src/
│   ├── main/
│   │   ├── java/uniamerica/abarbeirados/
│   │   │   └── AbarbeiradosApplication.java    # Classe de inicialização
│   │   └── resources/
│   │       └── application.properties          # Configuração da aplicação
│   └── test/
│       └── java/uniamerica/abarbeirados/
│           └── AbarbeiradosApplicationTests.java
├── mvnw / mvnw.cmd          # Maven Wrapper (Unix / Windows)
└── pom.xml                  # Dependências e build
```

O diretório do frontend Angular será adicionado quando a frente for iniciada.

---

## Como executar

### Pré-requisitos

- JDK 17 instalado
- Nenhuma instalação de Maven é necessária — o wrapper (`mvnw`) baixa a versão correta

### Pelo IntelliJ IDEA

1. **File → Open** e selecione a pasta do projeto (o IntelliJ detecta o `pom.xml`)
2. Confirme o SDK do projeto como **JDK 17** em *File → Project Structure → Project*
3. Execute a classe `AbarbeiradosApplication`

### Pelo terminal

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

> Rodar pelo terminal exige que o `java` esteja disponível no `PATH` e que a variável
> `JAVA_HOME` aponte para o JDK 17. Quem trabalha exclusivamente pelo IntelliJ não
> precisa dessa configuração — a IDE resolve o JDK internamente.

A aplicação sobe em `http://localhost:8080`.

### Banco de dados

Atualmente a aplicação usa **H2 em memória**, criado automaticamente na inicialização.
Não há credenciais a configurar, e **os dados são perdidos a cada reinicialização** —
comportamento esperado nesta fase.

A migração para PostgreSQL 18 será feita em etapa própria e trará a configuração de
conexão, o driver e a definição da estratégia de schema.

---

## Convenções

### Branches

| Branch | Finalidade |
|---|---|
| `main` | Código estável e integrado |
| `feature/<descricao>` | Novas funcionalidades |
| `fix/<descricao>` | Correções |

Trabalhe sempre em uma branch própria e integre à `main` via Pull Request, para que
o time consiga revisar antes da mesclagem.

### Commits

Padrão [Conventional Commits](https://www.conventionalcommits.org/pt-br/):

```
feat:     nova funcionalidade
fix:      correção de bug
refactor: refatoração sem mudança de comportamento
docs:     documentação
chore:    build, configuração, dependências
test:     testes
```

Exemplo: `feat: adiciona endpoint de listagem de clientes`

---

## Nota técnica — Spring Boot 4

O Spring Boot 4 renomeou e reorganizou vários artefatos em relação às versões 2 e 3.
**Tutoriais e vídeos mais antigos vão divergir do que está no `pom.xml`.**

| Spring Boot 2 / 3 | Spring Boot 4 |
|---|---|
| `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| `spring-boot-starter-test` | `spring-boot-starter-webmvc-test` e `spring-boot-starter-data-jpa-test` |
| Console H2 embutido no starter | `spring-boot-h2console` (módulo próprio) |

Copiar dependências de material antigo quebra o build. Ao consultar documentação,
prefira a [referência oficial da versão 4.1.0](https://docs.spring.io/spring-boot/4.1.0/).

---

## Próximos passos

- [ ] Receber e analisar o documento de especificação do demandante
- [ ] Modelar as entidades do domínio
- [ ] Migrar de H2 para PostgreSQL 18
- [ ] Implementar o CRUD e os 6 endpoints da primeira entrega
- [ ] Inicializar o projeto Angular 19
- [ ] Desenvolver as 2 telas da primeira entrega

---

Projeto acadêmico — Uniamerica, 2026.
