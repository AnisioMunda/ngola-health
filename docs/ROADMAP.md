# Ngola Health — Roadmap de Desenvolvimento

> Sistema de Gestão de Saúde · roadmap de desenvolvimento
> Estado: **PLANO v0.4** · Data: 2026-10-06 · Documentação em português · Código em inglês

Este documento é o mapa do desenvolvimento. Fica em `docs/ROADMAP.md` e cada tarefa vira uma *issue* no GitHub Projects.

---

## 1. Objectivo e princípios

Construir um sistema de gestão de saúde multi-hospital, passo a passo, com qualidade e segurança desde o primeiro commit. Os requisitos transversais estão na secção 2.

1. **Um PR = um commit na `main`**, completo e funcional (squash merge).
2. **Fundação antes de funcionalidade**: repositório, CI, testes, segurança e multi-hospital existem antes do primeiro módulo clínico.
3. **Testes e CI desde o primeiro dia**, e o CI corre sempre os testes.
4. **Só entra infra com caso de uso concreto** (nada de RabbitMQ/MinIO/Redis "para o futuro").
5. **Português onde as pessoas leem, inglês onde o código vive**: documentação, commits, comentários, interface e mensagens de erro em português; domínio (classes, tabelas, colunas, rotas, DTOs, papéis) e SQL em inglês.
6. **A documentação evolui com o código**, não no fim.
7. **Melhorar quando der**: sempre que houver uma funcionalidade, estrutura ou técnica melhor, adopta-se, desde que (a) fique registada na secção "Notas de melhoria" do PR, (b) seja uma mudança pequena e do mesmo âmbito da tarefa, e (c) mudanças maiores ganhem tarefa ou ADR próprios em vez de inflacionarem o PR.

---

## 2. Requisitos transversais de qualidade e segurança

Valem para **todos** os PRs e são referenciados nas tarefas pelo código `R<n>`.

| ID | Requisito | Como se garante | Tarefas |
|----|-----------|-----------------|---------|
| R1 | Segredos nunca entram no repositório | `.gitignore`, `.env.example` sem valores, `gitleaks` no CI, chaves por ficheiro montado | 0.12, 1.16, 5.4 |
| R2 | Tudo é negado por omissão | Regras por rota na configuração de segurança; Actuator e Swagger protegidos | 2.9 |
| R3 | Paciente e utilizador interno nunca se cruzam | Papel e *audience* próprios no portal; testes por perfil | 2.9, 2.10, 7.3 |
| R4 | A autenticação não mascara erros da aplicação | Filtro JWT só autentica; excepções seguem o tratamento global | 2.6 |
| R5 | Erros não expõem detalhes internos fora de `dev` | Formato ProblemDetail; mensagens controladas | 1.2, 1.5 |
| R6 | Dados de demonstração só em `dev`; senhas iniciais são temporárias | Contexto Liquibase `dev`; troca obrigatória | 2.14 |
| R7 | A auditoria funciona e é provada por teste | Teste de integração que verifica o registo | 2.12 |
| R8 | O CI corre sempre os testes; código só com caracteres ASCII em identificadores | Pipeline sem `-DskipTests`; script contra homóglifos | 1.7, 1.14, 1.17 |
| R9 | Configuração validada, sem duplicados nem valores fixos | `@ConfigurationProperties` com validação | 1.2 |
| R10 | Só entra infra com caso de uso concreto | ADR-0006 | 1.3 |
| R11 | Isolamento por hospital automático e testado | Filtro Hibernate + testes entre hospitais | 2.3, 2.10 |
| R12 | Um serviço por caso de uso | Classes pequenas e coesas; regras ArchUnit | 3.17, 4.12, 6.2 |
| R13 | Papéis são constantes, nunca texto solto | Enum de papéis e meta-anotações | 2.5 |
| R14 | Migrações SQL ordenadas e imutáveis | Secção 4.6 | 1.4, 3.1 |
| R15 | Tipos da API gerados; componentes pequenos | ADR-0007 | 7.6 |
| R16 | Dependências com licença compatível | Bibliotecas permissivas (ADR-0010) | 3.4 |
| R17 | Operações concorrentes são seguras (stock, slots, camas) | Restrições na BD + testes concorrentes | 3.13, 4.6, 4.12 |

---

## 3. Decisões de arquitectura (ADRs a registar)

Cada decisão vira um ficheiro em `docs/adr/` nas Tarefas 0.9 e 0.10. As ADR-0001 e ADR-0005 estão **confirmadas**; as restantes são propostas.

| ADR | Tema | Proposta | Alternativa |
|-----|------|----------|-------------|
| 0001 | Idioma | **Confirmado.** Português em: documentação, commits, comentários, interface e mensagens de erro mostradas ao utilizador. Inglês em: domínio e código (classes, tabelas, colunas, rotas, DTOs, papéis, SQL, ficheiros e pastas). Erros da API levam um `code` estável em inglês e a `message` em português | Tudo em português, incluindo o domínio (rejeitada) |
| 0002 | Stack e versões | Java 21 LTS · **Spring Boot 4.1.x** · Maven Wrapper · **Angular 22** · PostgreSQL 16+ · Liquibase. A Tarefa 1.1 faz uma verificação de compatibilidade (springdoc, jjwt, MapStruct, Lombok, biblioteca de PDF); se houver bloqueio, recuar para 3.5.x com plano de migração documentado | Ficar em 3.5.x / Angular 20 |
| 0003 | Multi-hospital | Coluna `hospital_id` + filtro automático do Hibernate (`@TenantId` ou `@Filter`) + testes de isolamento. Existe desde a Fase 2 | Schema por hospital |
| 0004 | Autenticação | JWT de acesso curto + refresh com rotação; revogação em PostgreSQL; portal do paciente com papel e *audience* próprios | Sessão com Redis |
| 0005 | Migrações | **Confirmado.** Liquibase com **SQL formatado**; o XML existe só como master de `<include>`. Um directório por módulo, ficheiros `NNN-descricao.sql`; dados de demonstração com `context:dev`. Detalhe na secção 4.6 | Changelogs em XML/YAML ou Flyway |
| 0006 | Infra | Compose inicial = PostgreSQL + backend + frontend. Redis, RabbitMQ, MinIO, SMTP e WebSocket só entram com ADR + caso de uso | Manter tudo desde já |
| 0007 | Frontend | Angular standalone + *signals*, Material, rotas lazy por funcionalidade, **tipos gerados a partir do OpenAPI** (acaba o desvio entre DTO e interface) | Tipos à mão |
| 0008 | Segredos | Nunca no repositório; `.env.example` sem valores; chave AGT por ficheiro montado; `gitleaks` no CI | — |
| 0009 | Facturação AGT | Assinatura RSA, cliente da API e consulta de estado. **Validar os requisitos vigentes da AGT antes da Fase 5** (regulamentação a confirmar) | — |
| 0010 | PDF | OpenPDF ou Apache PDFBox (licenças permissivas); evitar bibliotecas AGPL | Licença comercial de uma biblioteca AGPL |
| 0011 | Ortografia | Grafia anterior ao Acordo Ortográfico de 1990 (*projecto, facturação, activo, electrónico*), registada no guia de estilo | Acordo Ortográfico de 1990 |

---

## 4. Convenções

### 4.1 Formato do commit

```
<tipo>(<escopo>): <descrição curta>

[corpo: o quê e porquê — opcional]

[rodapé: Closes #12 · BREAKING CHANGE: …]
```

**Tipos permitidos** (apenas estes sete): `feat` · `fix` · `refactor` · `docs` · `style` · `test` · `chore`.
Alterações de pipeline usam `chore(ci)`; dependências usam `chore(deps)`.

**Regras**

- Primeira linha com **no máximo 72 caracteres**.
- Descrição em **minúsculas, sem ponto final**, no **infinitivo** ("adicionar", "corrigir", "remover"). Escolher uma forma e mantê-la: o infinitivo evita a ambiguidade do imperativo em português.
- Escopo em ASCII, sem acentos, em minúsculas.
- Um commit faz **uma coisa**, completa, e a `main` compila e passa nos testes a cada commit.
- **Nunca** misturar BD, API, interface e documentação no mesmo commit.

| Bom | Mau |
|-----|-----|
| `feat(auth): adicionar login com JWT de acesso e refresh` | `feat: adicionei o login` |
| `fix(farmacia): corrigir ordem FEFO com lotes da mesma validade` | `fix: bugs` |
| `docs(readme): adicionar guia de instalação do ambiente` | `docs: atualizações várias` |

**Escopos** (um por módulo ou área): `repo` `readme` `github` `ci` `docker` `infra` `bd` `deps` `backend` `frontend` `api` `seguranca` `auth` `utilizadores` `hospitais` `auditoria` `pacientes` `episodios` `triagem` `farmacia` `prescricoes` `laboratorio` `notificacoes` `agendamento` `internamento` `financeiro` `agt` `rh` `equipamentos` `dashboard` `relatorios` `portal` `telemedicina` `layout` `e2e` `release`. Em `docs`, o escopo é o assunto (`docs(arquitectura)`).

Os escopos são em português (fazem parte do commit) e as branches usam o mesmo escopo; o código usa os nomes em inglês. Correspondência: `utilizadores`→`users` · `hospitais`→`hospitals` · `auditoria`→`audit` · `pacientes`→`patients` · `episodios`→`episodes` · `triagem`→`triage` · `farmacia`→`pharmacy` · `prescricoes`→`prescriptions` · `laboratorio`→`laboratory` · `notificacoes`→`notifications` · `agendamento`→`scheduling` · `internamento`→`inpatient` · `financeiro`→`financial` · `rh`→`hr` · `equipamentos`→`equipment` · `relatorios`→`reports` · `telemedicina`→`telemedicine`.

### 4.2 Branches e Pull Requests

- `main`: só código estável. Protegida: sem *push* directo, PR obrigatório, CI verde, histórico linear.
- Desenvolvimento **trunk-based**: sem `develop`.
- Branch por tarefa: `<tipo>/<escopo>-<assunto>`, ex.: `feat/pacientes-api`, `fix/farmacia-fefo`.
- **Título do PR = mensagem final do commit** (já no formato da secção 4.1). Um workflow valida-o (Tarefa 0.6).
- Merge sempre **Squash and merge**, com o padrão do GitHub definido como "título do PR". Dentro da branch os commits são livres (`wip`, `fix typo`…): desaparecem no squash.
- Tamanho-alvo do PR: até ~400 linhas de lógica (migrações e *scaffolding* podem exceder).
- Releases: tags SemVer no fim de cada fase (`v0.2.0`, `v0.3.0`… `v1.0.0`).

### 4.3 Padrão de PR por módulo

Cada módulo funcional repete a mesma sequência, porque o Hibernate corre com `ddl-auto: validate` e a `main` nunca pode quebrar:

1. **BD** — ficheiro SQL do Liquibase registado no master XML (as tabelas existem, ninguém as usa ainda).
2. **API** — entidade, repositório, serviço, controlador, DTOs, validações, `@PreAuthorize` e **testes** no mesmo PR.
3. **Interface** — ecrãs Angular consumindo a API e tipos gerados.
4. **Docs** — um único PR `docs(...)` no fim de cada fase com as regras de negócio.

### 4.4 Definição de pronto (vai no template de PR)

- [ ] Compila e todos os testes passam; a cobertura não desce
- [ ] Lint e formatação limpos
- [ ] Migrações aplicam do zero **e** sobre a versão anterior
- [ ] Endpoints com `@PreAuthorize` e teste de autorização por perfil
- [ ] Dados filtrados por hospital, com teste de isolamento
- [ ] OpenAPI actualizado; documentação alterada se o comportamento mudou
- [ ] Sem segredos nem dados reais; título do PR válido

### 4.5 Ciclo de trabalho de cada tarefa

Issue do roadmap → branch → commits livres → PR com título final → CI verde → *squash merge* → apagar branch → marcar a tarefa como feita.

### 4.6 Convenção de migrações (Liquibase com SQL)

- **XML só como índice**: `db.changelog-master.xml` contém apenas `<include file="changes/<module>/NNN-descricao.sql" relativeToChangelogFile="true"/>`, **um por ficheiro, na ordem de dependência** (chaves estrangeiras). Sem `includeAll`, porque a ordem alfabética não respeita dependências entre módulos.
- **Ficheiros SQL formatados do Liquibase**, em `db/changelog/changes/<module>/`, numerados por módulo (`001-create-patients.sql`).
- Cada ficheiro começa por `--liquibase formatted sql`; cada alteração é um `--changeset`; toda a alteração reversível leva `--rollback`.
- Um changeset = uma alteração lógica. **Changeset já aplicado nunca se edita**: correcções são changesets novos.
- Dados de demonstração usam `context:dev`; só o perfil `dev` activa esse contexto.
- Nomes em inglês: tabelas em `snake_case` no plural (`patients`), toda a tabela com `hospital_id` quando pertence a um hospital, valores monetários em `NUMERIC`.
- O CI aplica **todas** as migrações num PostgreSQL vazio (Tarefa 1.7).

```sql
--liquibase formatted sql

--changeset ngola:patients-001-create-patients
CREATE TABLE patients (
    id          UUID PRIMARY KEY,
    hospital_id UUID         NOT NULL REFERENCES hospitals (id),
    full_name   VARCHAR(200) NOT NULL
);
--rollback DROP TABLE patients;
```

---

## 5. Estrutura-alvo do repositório

```
ngola-health/
├── backend/                       # Spring Boot (pacote base ao.ngolahealth)
│   └── src/main/
│       ├── java/ao/ngolahealth/
│       │   ├── config/            # segurança, OpenAPI, propriedades
│       │   ├── common/            # erros, paginação, auditoria, hospital actual
│       │   └── modules/<module>/  # api · application · domain · infrastructure
│       └── resources/db/changelog/
│           ├── db.changelog-master.xml        # só <include>
│           └── changes/<module>/NNN-*.sql     # SQL formatado do Liquibase
├── frontend/                      # Angular
│   └── src/app/
│       ├── core/                  # auth, interceptors, guards, API gerada
│       ├── shared/                # componentes e pipes reutilizáveis
│       └── features/<module>/     # texto da interface em português
├── infra/                         # compose, nginx, scripts de BD
├── docs/                          # em português
│   ├── README.md · ROADMAP.md
│   ├── adr/                       # decisões de arquitectura
│   ├── modules/                   # regras de negócio por módulo
│   └── agt/ · security/ · operations/
├── scripts/                       # utilitários (backup, verificação de caracteres)
├── .github/                       # workflows, templates de PR/issue
├── .editorconfig · .gitattributes · .gitignore · .env.example
└── CONTRIBUTING.md · SECURITY.md · CHANGELOG.md · README.md
```

Fronteiras entre módulos verificadas por **ArchUnit** (Tarefa 1.9): um módulo só fala com outro pela sua camada `application`.

---

## 6. Roadmap — tarefa por tarefa

Cada linha é **um PR = um commit na `main`**. A coluna *Commit* é a mensagem exacta do squash. Os números das tarefas são as referências usadas nas secções 2 e 3.

### Fase 0 — Fundação do repositório e documentação → `v0.1.0`

| # | Commit | Pronto quando |
|---|--------|---------------|
| 0.1 | `chore(repo): adicionar .gitignore, .editorconfig e .gitattributes` | `.gitignore` cobre `target/`, `node_modules/`, `.angular/`, `.env`, `.DS_Store`, IDEs; não ignora a si próprio |
| 0.2 | `docs(roadmap): adicionar roadmap de desenvolvimento e convenções` | `docs/ROADMAP.md` entra no repositório tal como está |
| 0.3 | `docs(readme): adicionar README inicial do Ngola Health` | Descreve o produto, a stack e o estado do projecto |
| 0.4 | `docs(contribuicao): adicionar guia de commits, branches e PRs` | `CONTRIBUTING.md` com a secção 4 deste documento |
| 0.5 | `chore(github): adicionar templates de PR e de issues` | Template de PR contém a definição de pronto e a secção "Notas de melhoria" |
| 0.6 | `chore(ci): validar o título do PR no formato de commit` | PR com título inválido falha o check |
| 0.7 | `docs(visao): documentar visão, escopo e glossário do projecto` | Glossário em português (paciente, episódio, internamento…) |
| 0.8 | `docs(requisitos): levantar requisitos e perfis de utilizador` | Perfis: administrador, gestor, médico, enfermeiro, recepcionista, farmacêutico, financeiro, técnico de laboratório, paciente |
| 0.9 | `docs(adr): registar decisões de idioma, convenções e multi-hospital` | ADR-0001, 0003, 0011 |
| 0.10 | `docs(adr): registar decisões de stack, infra, segurança e PDF` | ADR-0002, 0004–0010 |
| 0.11 | `docs(arquitectura): documentar arquitectura-alvo e estrutura do repo` | Diagrama de contexto + secção 5 |
| 0.12 | `docs(seguranca): adicionar SECURITY.md e política de segredos` | Como reportar falhas; regra "nunca segredos no git" (R1) |

### Fase 1 — Esqueleto técnico e CI → `v0.2.0`

| # | Commit | Pronto quando |
|---|--------|---------------|
| 1.1 | `chore(backend): inicializar projecto Spring Boot com Maven Wrapper` | `./mvnw verify` verde; `/actuator/health` responde UP; compatibilidade da ADR-0002 confirmada |
| 1.2 | `chore(backend): configurar perfis, propriedades e variáveis de ambiente` | `@ConfigurationProperties` validadas; sem duplicados nem valores fixos; detalhes de erro só em `dev` (R5, R9) |
| 1.3 | `chore(infra): adicionar PostgreSQL com Docker Compose e .env.example` | `docker compose up` sobe só a base de dados; `.env.example` sem valores (R10) |
| 1.4 | `chore(bd): configurar Liquibase com SQL e master XML de inclusão` | Master XML só com `<include>`; primeiro ficheiro SQL de exemplo aplica e reverte; contexto `dev` configurado; secção 4.6 aplicada (R14) |
| 1.5 | `feat(api): adicionar tratamento global de erros com ProblemDetail` | Erros em formato RFC 7807, mensagens em português; teste cobre 400, 404, 409 e 500 (R5) |
| 1.6 | `feat(api): adicionar documentação OpenAPI restrita ao perfil dev` | Swagger só em `dev`; contrato exportável para gerar tipos |
| 1.7 | `test(backend): configurar Testcontainers e teste de arranque` | Teste sobe PostgreSQL real, aplica todas as migrações do zero e o contexto Spring; pacote ASCII correcto (R8) |
| 1.8 | `chore(backend): adicionar Spotless e JaCoCo` | Formatação verificada no build; relatório de cobertura gerado |
| 1.9 | `test(arquitectura): adicionar regras ArchUnit entre módulos` | Falha se um módulo acede às camadas internas de outro |
| 1.10 | `chore(frontend): inicializar projecto Angular 22 com rotas lazy` | `ng build` verde; estrutura `core/shared/features`, tudo em inglês (ADR-0001) |
| 1.11 | `chore(frontend): configurar ESLint, Prettier e proxy para a API` | `npm run lint` verde; proxy `/api` funciona |
| 1.12 | `feat(layout): adicionar tema Material, locale pt-AO e página inicial` | Datas e moeda (AOA) formatadas em pt-AO |
| 1.13 | `chore(docker): adicionar Dockerfiles multi-stage e serviços no compose` | `docker compose up` sobe BD, backend e frontend; `healthcheck` em todos |
| 1.14 | `chore(ci): adicionar pipeline do backend com testes e cobertura` | **Corre os testes** (sem `-DskipTests`); segredos de CI no formato esperado pela aplicação (R8) |
| 1.15 | `chore(ci): adicionar pipeline do frontend com lint, testes e build` | Falha em lint ou teste vermelho |
| 1.16 | `chore(ci): adicionar verificação de segredos com gitleaks` | Falha se detectar chaves ou palavras-passe (R1) |
| 1.17 | `chore(ci): bloquear caracteres cirílicos e gregos no código-fonte` | Script em `scripts/` falha o CI em ficheiros e pastas com homóglifos (R8) |
| 1.18 | `docs(desenvolvimento): documentar como correr, testar e depurar` | Um programador novo consegue arrancar tudo só com o guia |

### Fase 2 — Identidade, acesso e multi-hospital → `v0.3.0`

| # | Commit | Pronto quando |
|---|--------|---------------|
| 2.1 | `feat(bd): criar tabela de hospitais` | Hospital padrão criado |
| 2.2 | `feat(hospitais): adicionar API de gestão de hospitais` | CRUD; papel de super-administrador definido na ADR-0003 |
| 2.3 | `feat(hospitais): isolar dados por hospital automaticamente` | Filtro Hibernate aplicado a toda a entidade com `hospital_id`; sem depender de cada serviço (R11) |
| 2.4 | `feat(bd): criar tabelas de utilizadores, perfis e permissões` | Papéis: `ADMIN`, `MANAGER`, `DOCTOR`, `NURSE`, `RECEPTIONIST`, `PHARMACIST`, `FINANCIAL`, `LAB_TECHNICIAN` (+ `PATIENT` na Fase 7) |
| 2.5 | `feat(utilizadores): adicionar API de gestão de utilizadores` | CRUD, activar/suspender, repor palavra-passe; papéis como enum/constantes, não texto solto (R13) |
| 2.6 | `feat(auth): adicionar login com JWT de acesso e refresh` | Filtro JWT **não** engole excepções a jusante (R4); refresh com rotação |
| 2.7 | `feat(auth): adicionar logout e revogação de tokens` | Token revogado é recusado; limpeza agendada de expirados |
| 2.8 | `feat(auth): adicionar troca obrigatória de senha e bloqueio de conta` | Troca obrigatória no primeiro acesso (`must_change_password`) e bloqueio temporário após tentativas falhadas |
| 2.9 | `feat(seguranca): aplicar default-deny, perfis por rota e CORS restrito` | Actuator e Swagger protegidos (R2); `/portal/**` só para o papel paciente; endpoints internos recusam token de paciente (R3) |
| 2.10 | `test(seguranca): adicionar testes de perfis e isolamento entre hospitais` | Matriz perfil × endpoint; utilizador do hospital A nunca vê dados do B (R3, R11) |
| 2.11 | `feat(bd): criar tabela de auditoria` | Campos: quem, o quê, entidade, id da entidade, antes/depois, IP, resultado |
| 2.12 | `feat(auditoria): registar acções de escrita de forma transversal` | **Teste prova que o registo acontece** (R7); `entityId` preenchido |
| 2.13 | `feat(auditoria): adicionar API de consulta de auditoria` | Filtros por utilizador, entidade, período; só administrador e gestor |
| 2.14 | `chore(bd): adicionar admin inicial e dados de demonstração em dev` | SQL com `context:dev`; admin inicial com senha temporária e troca obrigatória (R6) |
| 2.15 | `feat(auth): adicionar ecrã de login` | Ecrã claro e responsivo, em português |
| 2.16 | `feat(auth): adicionar interceptor JWT, refresh automático e guards` | Sessão expirada redirecciona para o login sem perder a rota |
| 2.17 | `feat(layout): adicionar shell com menu lateral por perfil` | Menu só mostra o que o perfil pode usar |
| 2.18 | `feat(utilizadores): adicionar ecrãs de listagem e formulário` | |
| 2.19 | `feat(hospitais): adicionar ecrã de gestão de hospitais` | Listagem e formulário, só para o papel autorizado pela ADR-0003 |
| 2.20 | `feat(auditoria): adicionar ecrã de consulta de auditoria` | |
| 2.21 | `docs(seguranca): documentar autenticação, perfis e multi-hospital` | |

### Fase 3 — Núcleo clínico → `v0.4.0`

| # | Commit | Pronto quando |
|---|--------|---------------|
| 3.1 | `feat(bd): criar tabela de pacientes` | Género, contactos e contacto de emergência; índice por hospital + BI/NIF; migração única e definitiva por tabela (R14) |
| 3.2 | `feat(pacientes): adicionar API de cadastro e pesquisa de pacientes` | CRUD, pesquisa paginada, validação de BI/NIF, aviso de possível duplicado |
| 3.3 | `feat(pacientes): adicionar listagem, pesquisa e formulário por tabs` | |
| 3.4 | `chore(deps): adicionar biblioteca de PDF conforme a ADR-0010` | PDF de teste gerado em teste automático (R16) |
| 3.5 | `feat(pacientes): gerar ficha do paciente em PDF` | Acentos e logótipo correctos |
| 3.6 | `feat(bd): criar tabela de episódios` | |
| 3.7 | `feat(episodios): adicionar API de episódios clínicos` | Abrir, actualizar, fechar; transições de estado validadas |
| 3.8 | `feat(episodios): adicionar listagem e formulário de episódios` | |
| 3.9 | `feat(bd): criar tabelas de triagem` | |
| 3.10 | `feat(triagem): adicionar API de triagem com prioridades Manchester` | Cor → tempo-alvo coberto por testes |
| 3.11 | `feat(triagem): adicionar ecrã de triagem e fila por prioridade` | |
| 3.12 | `feat(bd): criar tabelas de medicamentos, lotes e movimentos` | |
| 3.13 | `feat(farmacia): adicionar API de medicamentos e stock com FEFO` | Saída de stock sem condição de corrida (teste concorrente); alerta de validade (R17) |
| 3.14 | `feat(farmacia): adicionar ecrãs de medicamentos, stock e validades` | |
| 3.15 | `feat(farmacia): gerar inventário de stock em PDF` | |
| 3.16 | `feat(bd): criar tabelas de prescrições` | |
| 3.17 | `feat(prescricoes): adicionar API de prescrições médicas` | Validar contra o stock; um serviço por caso de uso (R12) |
| 3.18 | `feat(prescricoes): adicionar ecrãs de prescrição` | |
| 3.19 | `feat(bd): criar tabelas de pedidos e resultados de laboratório` | |
| 3.20 | `feat(laboratorio): adicionar API de pedidos e resultados` | Anexos de ficheiros ficam fora; só entram com ADR de armazenamento |
| 3.21 | `feat(laboratorio): adicionar ecrãs de pedidos e resultados` | |
| 3.22 | `docs(pacientes): documentar fluxo clínico e regras de negócio` | |

### Fase 4 — Notificações, agendamento e internamento → `v0.5.0`

| # | Commit | Pronto quando |
|---|--------|---------------|
| 4.1 | `feat(bd): criar tabela de notificações` | |
| 4.2 | `feat(notificacoes): adicionar API e serviço interno de notificações` | Outros módulos publicam eventos Spring; **sem broker** |
| 4.3 | `feat(notificacoes): adicionar sino, lista e página de notificações` | |
| 4.4 | `feat(bd): criar tabelas de horários e agendamentos` | |
| 4.5 | `feat(agendamento): adicionar API de horários e slots de médicos` | |
| 4.6 | `feat(agendamento): adicionar API de marcação e estados da consulta` | Impossível marcar duas vezes o mesmo slot (restrição na BD + teste concorrente) (R17) |
| 4.7 | `feat(agendamento): adicionar lembretes automáticos de consultas` | Tarefa agendada gera notificações |
| 4.8 | `feat(agendamento): adicionar horários e calendário no frontend` | |
| 4.9 | `feat(agendamento): adicionar marcação, lista e detalhe no frontend` | |
| 4.10 | `feat(bd): criar tabelas de enfermarias, camas e internamentos` | |
| 4.11 | `feat(internamento): adicionar API de enfermarias e camas` | |
| 4.12 | `feat(internamento): adicionar API de admissões, transferências e altas` | Uma cama = um paciente (restrição + teste); serviço dividido (R12, R17) |
| 4.13 | `feat(internamento): adicionar ecrãs de enfermarias e mapa de camas` | |
| 4.14 | `feat(internamento): adicionar admissões, detalhe e alta no frontend` | |
| 4.15 | `docs(agendamento): documentar agendamento, notificações e internamento` | |

### Fase 5 — Financeiro e facturação electrónica AGT → `v0.6.0`

> Antes de começar: rever os requisitos vigentes da AGT (ADR-0009) e **gerar as credenciais e a chave de assinatura do ambiente** (R1).

| # | Commit | Pronto quando |
|---|--------|---------------|
| 5.1 | `feat(bd): criar tabelas de facturas, itens e pagamentos` | Valores em `NUMERIC`, moeda AOA |
| 5.2 | `feat(financeiro): adicionar API de facturas, itens e pagamentos` | `BigDecimal` com arredondamento definido; totais e IVA testados |
| 5.3 | `feat(financeiro): adicionar ecrãs de facturas` | |
| 5.4 | `chore(agt): carregar chave privada e credenciais de segredo externo` | Nenhuma chave no repositório; falha ao arrancar em prod sem a chave (R1) |
| 5.5 | `feat(bd): adicionar campos de facturação electrónica AGT` | |
| 5.6 | `feat(agt): assinar documentos com RSA` | Testes com vectores conhecidos |
| 5.7 | `feat(agt): integrar a API sandbox da AGT com RestClient` | Falhas de rede tratadas com repetição e erro claro |
| 5.8 | `feat(agt): consultar estado das submissões por tarefa agendada` | |
| 5.9 | `feat(agt): gerar PDF da factura com QR Code` | |
| 5.10 | `feat(financeiro): mostrar estado AGT e emitir ou anular na interface` | |
| 5.11 | `docs(agt): documentar integração e fluxo de homologação` | |

### Fase 6 — Gestão e operações → `v0.7.0`

| # | Commit | Pronto quando |
|---|--------|---------------|
| 6.1 | `feat(bd): criar tabelas de turnos, folgas e ponto` | |
| 6.2 | `feat(rh): adicionar API de turnos e escalas` | Um serviço por caso de uso, sem classe "faz-tudo" (R12) |
| 6.3 | `feat(rh): adicionar API de folgas e aprovações` | |
| 6.4 | `feat(rh): adicionar API de controlo de ponto` | |
| 6.5 | `feat(rh): adicionar painel e turnos no frontend` | |
| 6.6 | `feat(rh): adicionar folgas e ponto no frontend` | |
| 6.7 | `feat(bd): criar tabelas de equipamentos e manutenções` | |
| 6.8 | `feat(equipamentos): adicionar API de equipamentos e manutenções` | Testes de serviço cobrem estados e manutenções |
| 6.9 | `feat(equipamentos): adicionar ecrã de equipamentos` | |
| 6.10 | `feat(dashboard): adicionar API de métricas do dashboard` | |
| 6.11 | `feat(dashboard): adicionar dashboard com gráficos` | |
| 6.12 | `feat(relatorios): adicionar API de relatórios de ocupação e financeiros` | |
| 6.13 | `feat(relatorios): adicionar dashboard executivo e ecrã de relatórios` | |
| 6.14 | `docs(rh): documentar RH, equipamentos, dashboard e relatórios` | |

### Fase 7 — Portal do paciente e telemedicina → `v0.8.0`

| # | Commit | Pronto quando |
|---|--------|---------------|
| 7.1 | `docs(adr): registar decisões do portal e da telemedicina` | Autenticação do portal; telemedicina com link externo ou WebRTC |
| 7.2 | `feat(bd): criar tabelas do portal do paciente` | |
| 7.3 | `feat(portal): adicionar registo e login do paciente` | Token com papel e *audience* próprios (R3) |
| 7.4 | `feat(portal): adicionar API de consultas, resultados e facturas` | Paciente só vê os seus dados (teste); âmbito final definido na ADR do portal |
| 7.5 | `feat(portal): adicionar ecrãs de login e registo do portal` | |
| 7.6 | `feat(portal): adicionar dashboard do paciente em componentes` | Dashboard dividido em componentes pequenos e reutilizáveis (R15) |
| 7.7 | `feat(bd): criar tabela de teleconsultas` | |
| 7.8 | `feat(telemedicina): adicionar API de teleconsultas` | |
| 7.9 | `feat(telemedicina): adicionar ecrã de teleconsultas` | |
| 7.10 | `docs(portal): documentar portal do paciente e telemedicina` | |

### Fase 8 — Endurecimento e lançamento → `v1.0.0`

| # | Commit | Pronto quando |
|---|--------|---------------|
| 8.1 | `test(e2e): adicionar Playwright com os fluxos críticos` | Login → paciente → episódio → prescrição → factura |
| 8.2 | `chore(ci): adicionar análise de dependências e Dependabot` | |
| 8.3 | `test(seguranca): adicionar varrimento de segurança automatizado` | OWASP ZAP *baseline* sobre o ambiente de teste |
| 8.4 | `chore(infra): adicionar compose de produção com nginx e TLS` | |
| 8.5 | `chore(infra): adicionar backup e restauro do PostgreSQL` | Restauro testado, não apenas o backup |
| 8.6 | `chore(backend): adicionar logs estruturados e métricas protegidas` | |
| 8.7 | `chore(bd): rever índices e consultas lentas` | Medido com dados de volume realista |
| 8.8 | `chore(bd): consolidar dados de demonstração` | |
| 8.9 | `docs(operacao): documentar implantação, backup e incidentes` | |
| 8.10 | `chore(release): preparar a versão 1.0.0` | Changelog e notas de versão |

**Total: 12 + 18 + 21 + 22 + 15 + 11 + 14 + 10 + 10 = 133 PRs.**

---

## 7. Módulos, fases e dependências

| Módulo | Fase | Depende de |
|--------|------|-----------|
| Hospitais | 2 | — |
| Utilizadores e autenticação | 2 | Hospitais |
| Auditoria | 2 | Utilizadores |
| Pacientes | 3 | Autenticação, Hospitais |
| Episódios | 3 | Pacientes |
| Triagem | 3 | Pacientes, Episódios |
| Farmácia | 3 | Hospitais |
| Prescrições | 3 | Episódios, Farmácia |
| Laboratório | 3 | Pacientes, Episódios |
| Notificações | 4 | Utilizadores |
| Agendamento | 4 | Pacientes, Utilizadores, Notificações |
| Internamento | 4 | Pacientes, Episódios |
| Financeiro | 5 | Pacientes, Episódios |
| Facturação electrónica AGT | 5 | Financeiro |
| Recursos humanos | 6 | Utilizadores |
| Equipamentos | 6 | Hospitais |
| Dashboard e relatórios | 6 | Todos os módulos anteriores |
| Portal do paciente | 7 | Pacientes, Agendamento, Laboratório, Financeiro |
| Telemedicina | 7 | Agendamento, Portal |

---

## 8. Riscos e pontos em aberto

| Risco | Mitigação |
|-------|-----------|
| Spring Boot 4.x ainda recente: bibliotecas podem não ter versão compatível | Verificação na 1.1; recuo documentado para 3.5.x |
| Regulamentação AGT pode mudar | ADR-0009: validar antes da Fase 5 |
| Dados de saúde são sensíveis (Lei de Protecção de Dados Pessoais de Angola, a validar com especialista) | Auditoria desde a Fase 2, isolamento por hospital testado, cifra em trânsito e backups cifrados |
| 133 PRs é um projecto longo | Cada fase termina numa versão utilizável e etiquetada; Fases 0–3 já entregam um núcleo clínico |

---

## 9. Passos manuais (fora do Git)

1. Criar o repositório `ngola-health` no GitHub.
2. Proteger a `main`; permitir **apenas** *Squash merge*, com mensagem padrão = título do PR; exigir CI verde; apagar branches após merge.
3. Criar o GitHub Project e importar as 133 tarefas como issues, uma por linha do roadmap.
4. Rever as decisões da secção 3 (as ADR-0001 e ADR-0005 já estão confirmadas).

---

## 10. Fontes consultadas para as versões (ADR-0002)

- Ciclo de vida do Spring Boot: https://versionlog.com/spring-boot/
- Ciclo de vida do Angular: https://ecorpit.com/angular-development-company/ · https://dev.to/endoflifeai/angular-end-of-life-the-18-month-treadmill-every-eol-date-1eml

Rever as versões no início da Fase 1: este documento foi escrito em 2026-10-06.

---

## 11. Histórico de revisões

| Versão | Data | Alteração |
|--------|------|-----------|
| 0.1 | 2026-10-06 | Plano inicial |
| 0.2 | 2026-10-06 | Idioma: domínio e código em inglês; Liquibase com SQL formatado e XML apenas como master (ADR-0001, ADR-0005, secção 4.6) |
| 0.3 | 2026-10-06 | Princípio 7 (melhorar quando der); tarefa 0.2 (roadmap entra no repositório) |
| 0.4 | 2026-10-06 | Documento autónomo: requisitos transversais R1–R17 e módulos por fase |
