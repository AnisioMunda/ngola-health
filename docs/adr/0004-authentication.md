# ADR-0004: Autenticação e sessões

- **Estado:** Aceite
- **Data:** 2026-10-07

## Contexto

O sistema terá utilizadores internos e, numa fase posterior, pacientes com
acesso a um portal. A autenticação precisa de limitar a duração dos tokens,
permitir terminar sessões e impedir que as credenciais do portal sejam usadas
para aceder às funções internas.

## Decisão

- Usar JWT de acesso de curta duração e refresh com rotação. A implementação
  actual usa HS256, com chave Base64 fornecida por `JWT_SECRET`; as durações
  padrão são 15 minutos para acesso e 7 dias para refresh.
- Guardar tokens revogados em PostgreSQL. A rotação revoga o refresh anterior;
  o logout revoga o refresh e, se recebido, o access token correspondente.
- Distinguir os tokens pelo claim `token_type` (`ACCESS`, `REFRESH` e
  `PATIENT_PORTAL`) e manter `PATIENT` separado dos perfis internos. A
  implementação actual ainda não usa um claim JWT `audience` nem uma chave de
  assinatura independente para o portal.
- Fazer com que o filtro JWT autentique e estabeleça o âmbito do hospital,
  sem ocultar excepções posteriores da aplicação.
- Exigir autorização por omissão e regras explícitas por controller/endpoint.
- Manter os tokens no `localStorage` no frontend actual; esta é uma decisão
  operacional provisória, com risco XSS, e não uma garantia de armazenamento
  seguro.

## Alternativa considerada

Usar sessões centralizadas em Redis. Não é a proposta inicial, pois introduz
uma dependência de infraestrutura que o projecto não deve adoptar sem um caso
de uso concreto.

## Consequências e revisões necessárias

- O ciclo de vida, a rotação, a revogação e a expiração de tokens devem ser
  cobertos por testes automatizados.
- Tokens do portal e internos não podem ser intercambiáveis; os filtros e
  regras de autorização devem validar o tipo e o perfil em todas as rotas.
- Segredos de assinatura não podem ser incluídos no código ou no repositório.
- O armazenamento no cliente, HTTPS, protecção XSS/CSRF e rotação de chaves
  devem ser revistos antes da produção. Ver
  [Segurança, perfis e isolamento multi-hospital](../security.md).
- A alteração de senha não revoga actualmente todas as sessões existentes;
  definir e implementar a revogação de sessões da conta como melhoria de
  segurança.
