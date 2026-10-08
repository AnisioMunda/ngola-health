# Ngola Health 1.0.0 — notas de versão

**Estado:** preparação; versão ainda não publicada.

Ngola Health é uma plataforma de gestão de saúde multi-hospital destinada a
apoiar operações clínicas e administrativas. Esta versão reúne funcionalidades
desenvolvidas ao longo das Fases 1–8. A existência de uma funcionalidade na
aplicação não constitui validação clínica, certificação regulamentar nem
autorização para tratar dados reais em produção.

## Destaques

- **Identidade e hospitais:** autenticação, perfis de acesso, âmbito
  multi-hospital e registo de auditoria.
- **Operações clínicas:** cadastro de pacientes, triagem, episódios, farmácia,
  prescrições e resultados laboratoriais. O sistema organiza registos; não
  diagnostica nem valida decisões clínicas.
- **Atendimento e gestão:** agendamento, notificações, internamentos,
  recursos humanos, equipamentos, dashboards e relatórios.
- **Serviços ao paciente:** portal com aprovação manual da identidade e acesso
  separado das funções internas.
- **Telemedicina:** criação de reuniões externas Microsoft Teams através do
  Microsoft Graph; a aplicação não transporta nem grava áudio ou vídeo.
- **Operação:** Compose de produção com Nginx/TLS, logs JSON, endpoints de
  métricas Actuator protegidos e backups locais PostgreSQL cifrados com `age`.
  Não é necessária conta S3 para desenvolvimento; o perfil de backup grava no
  directório `backups/` do projecto.

Consulte o [registo de alterações](../CHANGELOG.md) para o resumo técnico e
as verificações efectuadas.

## Requisitos e configuração

Consulte [development.md](development.md) para os requisitos de desenvolvimento
e [operations.md](operations.md) para deployment, verificação operacional,
backup, restauro e resposta a incidentes. A utilização em produção exige
segredos e integrações configurados no ambiente; não reutilize credenciais de
desenvolvimento.

## Limitações e condições de disponibilização

- **AGT:** o Compose de desenvolvimento aponta ao sandbox e mantém a integração
  desactivada por omissão. Com credenciais de homologação, pode ser activada
  localmente para testes sintéticos. O contrato, mapeamento fiscal e
  homologação precisam de confirmação antes de qualquer transmissão real ou
  alegação de conformidade.
- **Microsoft Teams:** requer configuração do tenant Entra, consentimento,
  política de acesso e organizadores licenciados. Os requisitos de privacidade
  e retenção do fornecedor devem ser aprovados pela instituição.
- **Produção:** emissão e renovação TLS reais dependem de domínio e ambiente
  público. Backups locais não são off-site nem substituem armazenamento externo
  e recuperação de produção.
- **Qualidade:** a execução do CI E2E no runner GitHub continua pendente.
  Protocolos clínicos, matriz completa de autorização e limitações de segurança
  documentadas devem ser revistos antes de uso com dados reais.
- **Continuidade:** a instituição deve designar responsáveis e contactos e
  aprovar objectivos de recuperação (RPO/RTO); este repositório não define
  escala de piquete ou SLA.

Por estas condições, estas notas não anunciam uma release publicada nem
recomendam a entrada em produção. Não foi criada a tag `v1.0.0`.
