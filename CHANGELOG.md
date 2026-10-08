# Registo de alterações

Este registo resume alterações relevantes por versão. A preparação de uma
versão não significa que tenha sido publicada ou aprovada para uso clínico ou
produção.

## [1.0.0] — preparação; não publicada

### Adicionado

- Gestão multi-hospital com utilizadores, autenticação, autorização e registo
  de auditoria.
- Fluxos de pacientes, triagem, episódios clínicos, farmácia, prescrições e
  laboratório.
- Agendamento, notificações e gestão de internamentos, camas e enfermarias.
- Facturação e integração técnica com a AGT, desactivada por omissão e ainda
  sujeita a validação do contrato oficial e homologação.
- Gestão de recursos humanos e equipamentos, dashboards e relatórios.
- Portal do paciente com aprovação de identidade e autenticação separada.
- Criação de teleconsultas Microsoft Teams através do Microsoft Graph, sujeita
  à configuração do tenant e às políticas de acesso do organizador.
- Execução local com Docker Compose, logs JSON, métricas Actuator protegidas
  e backups PostgreSQL cifrados com `age` para o directório local `backups/`.
- Índices PostgreSQL orientados por medições locais com dados sintéticos.

### Verificação registada

- Testes direccionados de prescrições e protecção de Actuator: 27 passaram.
- Testes Liquibase: 14 passaram.
- Backup e restauro cifrados foram exercitados com bases descartáveis e dados
  sintéticos; o perfil local grava ficheiros no computador e não requer S3.
- A configuração Compose e a sintaxe Nginx foram verificadas localmente.
- Os resultados e limites das medições de consultas estão em
  [development.md — benchmark PostgreSQL](docs/development.md#benchmark-de-consultas-postgresql).

### Antes da publicação ou entrada em produção

- A execução do CI E2E no runner GitHub ainda precisa de validação.
- Emissão/renovação TLS com domínio público depende da configuração de um
  ambiente de implantação; não é necessária para uso local.
- Testes contra o sandbox AGT podem ser feitos com credenciais de homologação
  e dados sintéticos. Confirmar o contrato, mapeamento fiscal e homologação
  antes de qualquer transmissão real.
- Rever os requisitos de privacidade, retenção e acesso do Microsoft Teams e
  configurar permissões e políticas no tenant.
- Validar clinicamente os protocolos e regras aplicáveis, completar a revisão
  de autorização e definir responsáveis, contactos, RPO e RTO institucionais.
- Backups locais não são uma cópia off-site nem substituem uma política de
  recuperação de produção.
- Rever as limitações de segurança e os restantes requisitos operacionais
  descritos em [security.md](docs/security.md) e
  [operations.md](docs/operations.md).

Não foi criada uma tag `v1.0.0`; esta entrada documenta a preparação da versão,
não uma publicação.
