# ADR-0003: Isolamento de dados entre hospitais

- **Estado:** Proposta
- **Data:** 2026-10-07

## Contexto

O Ngola Health destina-se a ser usado por vários hospitais. Dados clínicos e
operacionais de uma instituição não podem ficar acessíveis a utilizadores de
outra por omissão. O isolamento deve ser transversal e testado, não dependendo
de cada serviço se lembrar de acrescentar filtros às consultas.

## Proposta

- Associar cada registo pertencente a um hospital a um `hospital_id`.
- Aplicar o âmbito do hospital activo automaticamente na camada de persistência
  usando um mecanismo de multi-tenancy do Hibernate, a seleccionar durante a
  implementação técnica.
- Negar por omissão o acesso a dados sem um âmbito hospitalar válido.
- Provar o isolamento com testes entre hospitais, incluindo operações de
  leitura e escrita.
- Definir separadamente, antes da gestão de hospitais, se existe um
  super-administrador da plataforma, o seu âmbito e as suas permissões.

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

## Decisões pendentes

- Confirmar esta proposta antes da implementação da Fase 2.
- Seleccionar o mecanismo do Hibernate depois de verificar compatibilidade,
  gestão do contexto e comportamento em transacções.
- Definir o papel, as operações e as regras de auditoria do eventual
  super-administrador da plataforma.
