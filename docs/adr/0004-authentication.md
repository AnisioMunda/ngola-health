# ADR-0004: Autenticação e sessões

- **Estado:** Proposta
- **Data:** 2026-10-07

## Contexto

O sistema terá utilizadores internos e, numa fase posterior, pacientes com
acesso a um portal. A autenticação precisa de limitar a duração dos tokens,
permitir terminar sessões e impedir que as credenciais do portal sejam usadas
para aceder às funções internas.

## Proposta

- Usar tokens JWT de acesso de curta duração e tokens de refresh com rotação.
- Registar em PostgreSQL os tokens ou identificadores necessários para
  revogação e controlo de refresh.
- Separar o papel e o *audience* do paciente dos usados pelos utilizadores
  internos.
- Fazer com que o filtro JWT autentique, sem ocultar excepções posteriores da
  aplicação.
- Exigir autorização por omissão, com regras explícitas para cada rota.

## Alternativa considerada

Usar sessões centralizadas em Redis. Não é a proposta inicial, pois introduz
uma dependência de infraestrutura que o projecto não deve adoptar sem um caso
de uso concreto.

## Consequências esperadas

- O ciclo de vida, a rotação, a revogação e a expiração de tokens devem ser
  cobertos por testes.
- Tokens de pacientes e internos não podem ser intercambiáveis.
- Segredos de assinatura não podem ser incluídos no código ou no repositório.
- A estratégia de cookies, armazenamento no cliente e protecção contra
  ataques associados deve ser definida e revista durante a implementação da
  autenticação.

## Decisão pendente

Confirmar a proposta e completar as decisões de implementação antes da
Tarefa 2.6.
