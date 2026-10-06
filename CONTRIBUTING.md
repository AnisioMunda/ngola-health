# Guia de contribuição

Este guia define como organizar alterações, commits e pull requests no Ngola
Health. Cada contribuição deve corresponder a uma tarefa do
[roadmap](docs/ROADMAP.md) e manter a `main` estável.

## Idioma

Escrever em português a documentação, as mensagens de commit, os comentários,
a interface e as mensagens de erro apresentadas às pessoas. Usar inglês para
o domínio e o código: classes, tabelas, colunas, rotas, DTOs, papéis, SQL,
ficheiros e pastas.

## Mensagens de commit

Usar o formato:

```text
<tipo>(<escopo>): <descrição curta>

[corpo: o quê e porquê — opcional]

[rodapé: Closes #12 · BREAKING CHANGE: …]
```

Os únicos tipos permitidos são `feat`, `fix`, `refactor`, `docs`, `style`,
`test` e `chore`. Usar `chore(ci)` para alterações de pipeline e `chore(deps)`
para dependências.

Regras para a primeira linha:

- Ter no máximo 72 caracteres.
- Escrever a descrição em minúsculas, sem ponto final e no infinitivo.
- Escrever o escopo em ASCII, em minúsculas.
- Fazer um commit por alteração completa e manter a `main` compilável e com os
  testes aprovados.
- Não misturar alterações de base de dados, API, interface e documentação no
  mesmo commit.

| Bom | Mau |
| --- | --- |
| `feat(auth): adicionar login com JWT de acesso e refresh` | `feat: adicionei o login` |
| `fix(farmacia): corrigir ordem FEFO com lotes da mesma validade` | `fix: bugs` |
| `docs(readme): adicionar guia de instalação do ambiente` | `docs: atualizações várias` |

Escopos disponíveis: `repo`, `readme`, `github`, `ci`, `docker`, `infra`,
`bd`, `deps`, `backend`, `frontend`, `api`, `seguranca`, `auth`,
`utilizadores`, `hospitais`, `auditoria`, `pacientes`, `episodios`, `triagem`,
`farmacia`, `prescricoes`, `laboratorio`, `notificacoes`, `agendamento`,
`internamento`, `financeiro`, `agt`, `rh`, `equipamentos`, `dashboard`,
`relatorios`, `portal`, `telemedicina`, `layout`, `e2e` e `release`. Para
commits de documentação, o escopo identifica o assunto, por exemplo
`docs(arquitectura)`.

Os escopos e nomes de branch usam os termos portugueses definidos acima; o
código usa os nomes em inglês. Correspondências entre módulos:

| Escopo | Nome no código |
| --- | --- |
| `utilizadores` | `users` |
| `hospitais` | `hospitals` |
| `auditoria` | `audit` |
| `pacientes` | `patients` |
| `episodios` | `episodes` |
| `triagem` | `triage` |
| `farmacia` | `pharmacy` |
| `prescricoes` | `prescriptions` |
| `laboratorio` | `laboratory` |
| `notificacoes` | `notifications` |
| `agendamento` | `scheduling` |
| `internamento` | `inpatient` |
| `financeiro` | `financial` |
| `rh` | `hr` |
| `equipamentos` | `equipment` |
| `relatorios` | `reports` |
| `telemedicina` | `telemedicine` |

## Branches e pull requests

- `main` contém apenas código estável. Não se faz push directo; as alterações
  entram por pull request com CI verde e histórico linear.
- O desenvolvimento segue o modelo trunk-based: não existe branch `develop`.
- Criar uma branch por tarefa, usando `<tipo>/<escopo>-<assunto>`, por exemplo
  `feat/pacientes-api` ou `fix/farmacia-fefo`.
- O título do pull request é a mensagem final do commit de squash e deve
  obedecer às convenções de commit. Um workflow valida o título.
- Integrar sempre com **Squash and merge**, configurando o GitHub para usar o
  título do pull request. Os commits intermédios da branch podem ser livres,
  pois não ficam no histórico final.
- Procurar manter cada pull request abaixo de aproximadamente 400 linhas de
  lógica. Migrações e scaffolding podem exceder esse valor.
- Criar tags SemVer no fim de cada fase, conforme o roadmap (por exemplo,
  `v0.2.0`, `v0.3.0` e `v1.0.0`).

## Organização dos pull requests

Para cada módulo funcional, seguir esta sequência, sem deixar a `main` num
estado incompatível com `ddl-auto: validate`:

1. **Base de dados:** adicionar o SQL do Liquibase e incluí-lo no master. As
   tabelas passam a existir, mas ainda não são usadas pela aplicação.
2. **API:** implementar entidade, repositório, serviço, controlador, DTOs e
   validações, incluindo `@PreAuthorize` e testes.
3. **Interface:** implementar os ecrãs Angular que consomem a API e usar os
   tipos gerados.
4. **Documentação:** no fim da fase, documentar as regras de negócio num pull
   request `docs(...)`.

## Definição de pronto

Antes de pedir revisão, confirmar os itens aplicáveis:

- [ ] A aplicação compila e todos os testes passam; a cobertura não diminui.
- [ ] Lint e formatação estão limpos.
- [ ] As migrações aplicam-se numa base vazia e sobre a versão anterior.
- [ ] Os endpoints têm `@PreAuthorize` e testes de autorização por perfil.
- [ ] Os dados são filtrados por hospital e existe teste de isolamento.
- [ ] O OpenAPI está actualizado e a documentação reflecte alterações de
      comportamento.
- [ ] Não foram adicionados segredos nem dados reais; o título do pull request
      segue o formato de commit.

## Ciclo de trabalho

Seguir este ciclo para cada tarefa:

1. Escolher a issue correspondente ao roadmap.
2. Criar uma branch para a tarefa.
3. Desenvolver e validar a alteração com commits intermédios.
4. Abrir um pull request com o título do commit final.
5. Aguardar CI verde e revisão.
6. Integrar com squash merge.
7. Apagar a branch e marcar a tarefa como concluída.

## Migrações da base de dados

As migrações usam Liquibase com SQL formatado. O XML é apenas o índice:
`db.changelog-master.xml` contém um `<include>` por ficheiro, na ordem das
dependências e das chaves estrangeiras. Não usar `includeAll`, porque a ordem
alfabética não garante a ordem correcta entre módulos.

Guardar os ficheiros em `db/changelog/changes/<module>/`, numerados por módulo
no formato `NNN-descricao.sql`. Cada ficheiro começa com
`--liquibase formatted sql`; cada alteração lógica tem o seu próprio
`--changeset` e, quando reversível, uma instrução `--rollback`.

Depois de aplicado, um changeset é imutável. Fazer correcções com um novo
changeset. Dados de demonstração devem usar `context:dev`, activado apenas no
perfil `dev`. Usar nomes de tabela em inglês, no plural e em `snake_case`
(por exemplo, `patients`); incluir `hospital_id` nas tabelas pertencentes a
um hospital e usar `NUMERIC` para valores monetários.

O CI deve aplicar todas as migrações numa instância PostgreSQL vazia.
