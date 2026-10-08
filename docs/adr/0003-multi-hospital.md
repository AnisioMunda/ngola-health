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
  com `@TenantId` do Hibernate e um `CurrentTenantIdentifierResolver`.
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
- O login global por email e a consulta de contas do portal são fluxos de
  identidade: podem resolver uma conta antes de conhecer o hospital, mas só
  em contexto explícito de plataforma e sem expor consultas clínicas fora do
  hospital associado ao token.
- Os eventos de auditoria podem ser globais e ter `hospital_id` nulo; a sua
  consulta continua a ser filtrada explicitamente pelo serviço de auditoria.

## Alternativas consideradas

- **Schema separado por hospital:** não proposto, pois aumenta a complexidade
  operacional e das migrações.
- **Filtro manual em cada serviço:** rejeitado como mecanismo principal, pois
  permite omissões acidentais e torna o isolamento dependente de cada chamada.

## Consequências esperadas

- Entidades que pertencem a um hospital terão de transportar a associação
  hospitalar através do `TenantScopedEntity`; tabelas de detalhe sem coluna
  própria recebem `hospital_id` e são preenchidas a partir do registo pai.
- O contexto do hospital terá de ser estabelecido e validado em cada pedido
  autenticado antes do acesso aos dados.
- Sem hospital activo, o resolver usa um identificador que não corresponde a
  nenhum hospital; o acesso global só existe durante fluxos explícitos de
  plataforma e é confirmado pela role `SUPER_ADMIN`.
- Testes de integração terão de provar que uma conta de um hospital não lê nem
  altera os dados de outro.
- As contas do portal são resolvidas globalmente pelo email único apenas nos
  fluxos de login/registo; o token resultante inclui o hospital e os dados do
  paciente ficam sujeitos ao filtro automático.

## Decisões técnicas pendentes

- Provar a propagação do contexto de hospital em tarefas assíncronas de
  auditoria e nos restantes fluxos que forem acrescentados.
