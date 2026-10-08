# Segurança, perfis e isolamento multi-hospital

Este documento descreve os controlos presentes no código actual. Não substitui
uma avaliação de segurança, revisão jurídica ou validação para produção. O
backend é a fronteira de segurança; os menus e guards do frontend servem para
orientar a utilização, não para autorizar operações.

## Autenticação e sessões

- A API está sob o prefixo `/api`. O login interno é feito por
  `POST /api/auth/login`; o portal do paciente tem endpoints de registo e login
  próprios.
- As senhas são guardadas com BCrypt, configurado com 12 _rounds_. A criação
  normal de utilizadores exige pelo menos 8 caracteres, com letras maiúsculas,
  minúsculas e números. A alteração de senha exige pelo menos 12 caracteres e
  uma senha diferente da actual.
- Após cinco tentativas de login falhadas, a conta fica bloqueada por 15
  minutos. Uma autenticação bem-sucedida limpa o contador e o bloqueio.
- Os tokens são JWT assinados com HS256. `JWT_SECRET` tem de conter uma chave
  Base64 com material criptográfico suficiente. O token de acesso tem duração
  padrão de 15 minutos (`JWT_EXPIRATION_MS`) e o de refresh, 7 dias
  (`JWT_REFRESH_EXPIRATION_MS`).
- Os refresh tokens são rodados: o token anterior é colocado na lista de
  revogação antes de ser emitido o seguinte. O logout revoga o refresh token e,
  quando enviado, também o token de acesso correspondente. Os tokens expirados
  são removidos periodicamente da lista.
- O frontend guarda os tokens no `localStorage`. Isto expõe as sessões a
  código executado por uma vulnerabilidade XSS; não se deve tratar esta escolha
  como equivalente a cookies `HttpOnly`. A prevenção de XSS, a política de
  conteúdo e a revisão de armazenamento continuam necessárias.

## Perfis e autorização

O backend aplica autorização global e regras específicas por endpoint com
Spring Security e `@PreAuthorize`. As regras mais restritivas prevalecem; a
presença de um item no menu não concede acesso.

| Perfil                                      | Âmbito e responsabilidade principal                                                                                                   |
| ------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| `SUPER_ADMIN`                               | Plataforma: gerir hospitais e respectivas activações; não representa um administrador de hospital nem recebe `hospital_id`.           |
| `ADMIN`                                     | Hospital: administrar utilizadores e operações explicitamente permitidas ao administrador.                                            |
| `MANAGER`                                   | Hospital: consultar utilizadores, auditoria e relatórios onde autorizado; as mutações de utilizadores continuam reservadas a `ADMIN`. |
| `DOCTOR`, `NURSE`, `RECEPTIONIST`           | Utilização clínica e operacional conforme as regras de cada endpoint.                                                                 |
| `PHARMACIST`, `FINANCIAL`, `LAB_TECHNICIAN` | Acesso às funções de farmácia, financeiras e laboratoriais, respectivamente, conforme as regras de cada endpoint.                     |
| `PATIENT`                                   | Portal do paciente; não é um perfil interno e não pode usar os endpoints internos.                                                    |

Os endpoints de gestão de hospitais — incluindo a listagem que também mostra
hospitais inactivos, criação, actualização e activação/desactivação — exigem
`SUPER_ADMIN`. A gestão de utilizadores permite leitura a `ADMIN` e `MANAGER`,
mas criação, actualização, alteração de estado e reposição de senha exigem
`ADMIN`. A consulta de auditoria exige `ADMIN` ou `MANAGER`.

As regras completas são definidas nos controllers e em
[`SecurityConfig`](../backend/src/main/java/ao/hospitalao/config/SecurityConfig.java).
Actuator e documentação OpenAPI são restritos a `ADMIN`, `MANAGER` e
`SUPER_ADMIN`. O CORS aceita apenas a origem exacta configurada em
`APP_BASE_URL`; não usar origens abertas em produção.

Os endpoints com dados clínicos detalhados de triagem — fila, histórico,
detalhe, criação e alteração de estado/prioridade — exigem `ADMIN`, `DOCTOR`
ou `NURSE`. `MANAGER` só pode consultar estatísticas agregadas; `RECEPTIONIST`
não recebe acesso à triagem clínica.

Na farmácia, `ADMIN` e `PHARMACIST` podem criar medicamentos, receber stock e
dispensar. `MANAGER` tem acesso apenas de leitura a medicamentos, lotes e
alertas de validade; `DOCTOR` e `NURSE` podem consultar apenas o catálogo de
medicamentos.

## Isolamento entre hospitais

As entidades pertencentes a um hospital que estendem `TenantScopedEntity` usam
`hospital_id` e o filtro automático do Hibernate (`@TenantId`). Depois de
validar o JWT, o filtro de autenticação estabelece o hospital indicado no
token. Sem hospital válido, o resolver usa um UUID sentinela que não
corresponde a um hospital. O âmbito de plataforma só é activado explicitamente
para fluxos que precisam de resolver uma identidade global, como login e
refresh, e é removido no fim do pedido/fluxo.

O `SUPER_ADMIN` tem uma claim de plataforma e não uma claim de hospital. A
claim de plataforma activa o acesso raiz explícito do resolver Hibernate; por
isso, a separação entre gestão da plataforma e dados clínicos também depende de
regras por endpoint que excluam esse perfil quando apropriado. Não presumir
que o filtro de tenant, por si só, limita um `SUPER_ADMIN` a um hospital. O
portal usa o tipo de token `PATIENT_PORTAL`, o perfil `PATIENT` e o hospital
associado ao paciente; o filtro recusa tokens do portal nos endpoints internos.

O isolamento automático reduz o risco de consultas sem filtro manual, mas não
dispensa testes de integração entre hospitais nem revisão de novos fluxos,
especialmente tarefas assíncronas e operações de plataforma.

## Auditoria e configuração operacional

As acções de escrita são registadas na tabela de auditoria com utilizador,
hospital quando aplicável, acção, entidade, identificador, resultado e dados de
pedido disponíveis. A API de consulta e as estatísticas estão restritas a
`ADMIN` e `MANAGER`; as consultas são filtradas pelo âmbito hospitalar quando
este existe. Os registos podem conter dados pessoais e endereços IP: limitar o
acesso à base de dados e aos backups e evitar incluir senhas, tokens ou dados
clínicos desnecessários em descrições e mensagens de erro.

Para desenvolvimento local, consulte
[`development.md`](development.md): o administrador de plataforma de exemplo
é criado apenas no contexto `dev`, a senha é fornecida localmente e exige
alteração no primeiro acesso. Base64 é codificação, não cifragem. Segredos
devem permanecer fora do repositório e ser fornecidos por um gestor de segredos
em ambientes partilhados.

## Limitações conhecidas e requisitos antes de produção

- O frontend usa `localStorage`; considerar uma estratégia com cookies
  `HttpOnly`, `Secure` e `SameSite` após uma análise de CSRF e do fluxo de
  autenticação.
- Servir a aplicação e a API exclusivamente por HTTPS e configurar `APP_BASE_URL`
  para a origem exacta do frontend.
- Em produção, os logs da aplicação são emitidos em JSON e os níveis DEBUG ficam
  desactivados; não incluir identidades, dados clínicos, credenciais ou tokens
  nas mensagens. Os endpoints `/api/actuator/metrics` e
  `/api/actuator/prometheus` exigem um papel administrativo e não devem ser
  expostos directamente a scrapers sem autenticação.
- A alteração de senha não revoga actualmente todos os tokens já emitidos para
  a conta. O refresh token permanece utilizável até expirar ou ser revogado;
  este comportamento deve ser revisto antes de tratar a alteração de senha
  como encerramento de todas as sessões.
- A rotação de `JWT_SECRET` invalida tokens assinados com a chave anterior;
  planear a rotação e a reautenticação dos utilizadores.
- Rever retenção e acesso aos registos de auditoria, protecção de backups,
  cabeçalhos de segurança, limites de pedidos e monitorização para cada
  ambiente de produção.
- Rever controllers que aceitam `isAuthenticated()` sem uma restrição de
  hospital/perfil: o papel `SUPER_ADMIN` está autorizado na regra HTTP global e
  activa o acesso raiz do Hibernate.

As decisões arquitecturais de identidade e multi-hospital estão registadas em
[ADR-0004](adr/0004-authentication.md) e [ADR-0003](adr/0003-multi-hospital.md).
