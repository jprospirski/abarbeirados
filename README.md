# Abarbeirados

Sistema de gestão para barbearia — projeto acadêmico do 4º período de
Engenharia de Software / Análise e Desenvolvimento de Sistemas da **Uniamerica**.

O projeto atende a uma demanda real encaminhada à faculdade. O documento de
especificação do demandante ainda não foi formalizado, portanto as entidades e
regras de negócio seguem em aberto — o desenvolvimento avança sobre o domínio
(barbearia) e os critérios técnicos já definidos para a entrega.

---

## Status

| Frente | Situação |
|---|---|
| Backend (Spring Boot) | CRUD de Cliente e Serviço concluído; Agendamento pendente |
| Frontend (Angular) | Tela de agendamento implementada; listagem pendente |
| Banco de dados | H2 em memória (provisório) → PostgreSQL 18 |
| Especificação do demandante | Pendente |

---

## Entrega Parcial 1 — Aplicação com CRUD completo

### Critérios de avaliação

| # | Critério | Peso | Status |
|---|---|:---:|:---:|
| 1 | Projeto Spring Boot com pacotes ajustados ao projeto (não usar `demo`) | 2 | ✅ |
| 2 | CRUD completo | 4 | ⬜ |
| 3 | Mínimo de 6 endpoints — verbos HTTP adequados | 1 | ⬜ |
| 4 | Mínimo de 6 endpoints — códigos HTTP adequados | 1 | ⬜ |
| 5 | Mínimo de 6 endpoints — retorno estruturado | 1 | ⬜ |
| 6 | Ao menos um `@PathVariable`, um `@RequestParam` e um `@RequestBody` | 1 | ⬜ |
| 7 | Estruturação MVC adequada ao projeto | 3 | ⬜ |
| 8 | Uso de Lombok e `record` | 2 | ⬜ |
| 9 | Uso de DTOs | 2 | ⬜ |
| | **Total** | **17** | |

> **Critério 1 já atendido:** o pacote base é `uniamerica.abarbeirados`, definido na
> geração do projeto. Nenhum vestígio de `com.example.demo`.

### O que deve ser entregue

- Link do repositório no GitHub
- Arquivo ZIP com o `src` da aplicação

> Esta entrega é **exclusivamente backend** — o documento não menciona telas.
> O frontend Angular entra em etapa posterior.

---

## Equipe

- João Pedro Rospirski Pegorini
- Cauã Buch Domingues
- Christopher Adam
- Leonardo Barth

---

## Tecnologias

| Camada | Tecnologia | Versão | Observação |
|---|---|---|---|
| Backend | Java | 17 | Baseline do Spring Boot 4.x |
| | Spring Boot | 4.1.0 | Web MVC + Data JPA |
| | Maven | 3.9.16 | Via wrapper (`mvnw`), sem instalação |
| | Lombok | — | Gerenciado pelo Spring Boot |
| Banco | H2 | — | Provisório, em memória |
| | PostgreSQL | 18 | Definitivo — não integrado ainda |
| Frontend | Angular | 19 | Não inicializado |
| | Node.js | 20.11+ | Ver nota abaixo |
| Apoio | DBeaver, IntelliJ IDEA | — | Cliente do banco e IDE do time |

> **Node.js:** o Angular 19 exige `^18.19.1`, `^20.11.1` ou `^22.x`. A linha 14 não é
> suportada e falha na instalação do Angular CLI. Use **20.11 LTS ou superior**.

> **Spring Boot 4** renomeou artefatos: `starter-web` virou `starter-webmvc`,
> `starter-test` foi dividido em `webmvc-test` e `data-jpa-test`, e o console H2 é o
> módulo `spring-boot-h2console`. Tutoriais de Boot 2/3 quebram o build — consulte a
> [documentação da 4.1.0](https://docs.spring.io/spring-boot/4.1.0/).

---

## Como executar

**Pelo IntelliJ IDEA** (caminho padrão do time): abra a pasta do projeto, confirme o
SDK como **JDK 17** em *File → Project Structure → Project* e execute
`AbarbeiradosApplication`.

**Pelo terminal:**

```bash
./mvnw spring-boot:run      # Linux / macOS
mvnw.cmd spring-boot:run    # Windows
```

> O terminal exige `java` no `PATH` e `JAVA_HOME` apontando para o JDK 17. Quem usa
> apenas o IntelliJ não precisa configurar nada — a IDE resolve o JDK internamente.

A aplicação sobe em `http://localhost:8080`.

**Banco de dados:** o H2 em memória é criado na inicialização, sem credenciais a
configurar. Os dados são perdidos a cada reinicialização — esperado nesta fase.

---

## Convenções

**Branches:** `main` (estável) · `feature/<descricao>` · `fix/<descricao>`.
Trabalhe em branch própria e integre via Pull Request.

**Commits:** [Conventional Commits](https://www.conventionalcommits.org/pt-br/) —
`feat`, `fix`, `refactor`, `docs`, `chore`, `test`.
Exemplo: `feat: adiciona endpoint de listagem de clientes`

---

## Próximos passos

- [ ] Receber o documento de especificação do demandante
- [ ] Modelar as entidades do domínio
- [ ] Estruturar as camadas MVC (`model`, `repository`, `service`, `controller`)
- [ ] Implementar DTOs como `record`
- [ ] Implementar o CRUD e os 6 endpoints
- [ ] Migrar de H2 para PostgreSQL 18
- [x] Inicializar o frontend Angular 19
- [x] Tela de agendamento (frontend)
- [ ] Tela de listagem (frontend)
- [ ] Implementar o domínio Agendamento no backend e ligar a tela à API

---

Projeto acadêmico — Uniamerica, 2026.
