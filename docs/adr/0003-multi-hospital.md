# ADR-0003: Isolamento de dados entre hospitais

- **Estado:** Aceite
- **Data:** 2026-11-04

## Contexto

O Ngola Health destina-se a ser usado por vários hospitais. Dados clínicos e
operacionais de uma instituição não podem ficar acessíveis a utilizadores de
outra por omissão. O isolamento deve ser transversal e testado, não dependendo
de cada serviço se lembrar de acrescentar filtros às consultas.

## Decisão

- Associar cada registo pertencente a um hospital a um `hospital_id`.
- Aplicar o âmbito do hospital activo automaticamente na camada de persistência
  usando um mecanismo de multi-tenancy do Hibernate, a seleccionar durante a
  implementação técnica.
- Negar por omissão o acesso a dados sem um âmbito hospitalar válido.
- Provar o isolamento com testes entre hospitais, incluindo operações de
  leitura e escrita.
- Existe um papel `SUPER_ADMIN`, de âmbito de plataforma e sem associação a um
  hospital. É distinto do `ADMIN`, que administra apenas o seu hospital.
- O `SUPER_ADMIN` pode criar, actualizar, consultar e activar/desactivar
  hospitais. A desactivação é a operação de remoção: não se apagam hospitais
  fisicamente porque podem estar referenciados por dados clínicos e operacionais.
- Apenas um operador de confiança pode provisionar o primeiro
  `SUPER_ADMIN`, fora dos endpoints públicos. A API normal de utilizadores não
  permite a um administrador hospitalar atribuir esse papel.
- A listagem normal mostra hospitais activos; apenas `SUPER_ADMIN` pode incluir
  hospitais desactivados na listagem.
- As mutações de hospitais são registadas na auditoria como entidade
  `HOSPITAL`.

## Alternativas consideradas

- **Schema separado por hospital:** não proposto, pois aumenta a complexidade
  operacional e das migrações.
- **Filtro manual em cada serviço:** rejeitado como mecanismo principal, pois
  permite omissões acidentais e torna o isolamento dependente de cada chamada.

## Consequências esperadas

- Entidades que pertencem a um hospital terão de transportar a associação
  hospitalar definida pelo modelo de dados.
- O contexto do hospital terá de ser estabelecido e validado em cada pedido
  autenticado antes do acesso aos dados.
- Testes de integração terão de provar que uma conta de um hospital não lê nem
  altera os dados de outro.
- A escolha concreta entre `@TenantId`, `@Filter` ou outro mecanismo suportado
  pelo Hibernate permanece por validar na fundação técnica.

## Decisões técnicas pendentes

- Seleccionar o mecanismo do Hibernate depois de verificar compatibilidade,
  gestão do contexto e comportamento em transacções.
