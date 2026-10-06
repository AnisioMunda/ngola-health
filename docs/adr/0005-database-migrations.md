# ADR-0005: Migrações da base de dados

- **Estado:** Aceite
- **Data:** 2026-10-07

## Contexto

As alterações ao esquema da base de dados precisam de ser ordenadas,
reproduzíveis e aplicáveis tanto a instalações novas como a versões já
existentes. O histórico das alterações tem de permanecer auditável e
compatível com a execução contínua do projecto.

## Decisão

- Usar Liquibase com ficheiros SQL formatados.
- Manter o XML apenas como índice: `db.changelog-master.xml` contém um
  `<include>` por ficheiro SQL, com caminhos relativos e na ordem das
  dependências.
- Organizar os ficheiros em
  `db/changelog/changes/<module>/NNN-descricao.sql`.
- Começar cada ficheiro com `--liquibase formatted sql`; definir cada alteração
  lógica num `--changeset` e incluir `--rollback` quando a alteração for
  reversível.
- Não editar um changeset depois de aplicado; introduzir correcções em novos
  changesets.
- Usar `context:dev` para dados de demonstração e activar esse contexto apenas
  no perfil `dev`.
- Fazer o CI aplicar todas as migrações numa instância PostgreSQL vazia.

## Alternativas consideradas

- Changelogs em XML ou YAML e Flyway foram considerados, mas rejeitados em
  favor de SQL legível e executado directamente pela base de dados.
- `includeAll` foi rejeitado porque a ordem alfabética não garante a ordem das
  dependências entre módulos.

## Consequências

- A ordem dos `<include>` tem de respeitar as dependências e chaves
  estrangeiras.
- Alterações ao esquema exigem um novo changeset e testes de aplicação.
- Tabelas pertencentes a um hospital incluem `hospital_id`; nomes de tabelas
  são em inglês, plurais e `snake_case`; valores monetários usam `NUMERIC`.
