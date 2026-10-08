# ADR-0004: Autenticação e sessões

- **Estado:** Aceite
- **Data:** 2026-10-08

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
  `PATIENT_PORTAL`) e manter `PATIENT` separado dos perfis internos.
- Exigir o claim JWT padrão `aud` em todos os tokens: `hospital-api` para
  utilizadores internos e `patient-portal` para pacientes. O papel `PATIENT`,
  a audiência e o tipo do token têm de ser validados em conjunto; um token
  válido num contexto nunca autentica no outro.
- Vincular a identidade do portal ao `patient_id` do registo autenticado. O
  endereço electrónico serve para iniciar sessão, não para determinar o
  âmbito dos dados consultados.
- O registo público cria uma conta pendente, sem emitir token. A conta só é
  activada depois de um administrador, gestor ou recepcionista do mesmo
  hospital confirmar presencialmente a identidade do paciente e o contacto
  indicado. A lista de pedidos e a aprovação ficam sempre limitadas ao
  hospital autenticado.
- Manter inicialmente a chave HS256 partilhada em `JWT_SECRET`. A separação
  obrigatória é feita por tipo, papel e audiência; uma chave independente
  para o portal fica adiada até existir uma necessidade operacional ou uma
  revisão do modelo de ameaças.
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
  regras de autorização devem validar tipo, perfil e audiência em todas as
  rotas. Os tokens emitidos antes da validação obrigatória de `aud` são
  recusados; os utilizadores têm de iniciar sessão novamente.
- Contas pendentes não podem iniciar sessão; a verificação do identificador
  do paciente, a aprovação por pessoal autorizado e o isolamento entre
  hospitais devem ter testes automatizados.
- Segredos de assinatura não podem ser incluídos no código ou no repositório.
- O armazenamento no cliente, HTTPS, protecção XSS/CSRF e rotação de chaves
  devem ser revistos antes da produção. Ver
  [Segurança, perfis e isolamento multi-hospital](../security.md).
- A alteração de senha não revoga actualmente todas as sessões existentes;
  definir e implementar a revogação de sessões da conta como melhoria de
  segurança.
