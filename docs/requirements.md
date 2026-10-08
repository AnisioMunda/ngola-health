# Requisitos e perfis de utilizador

Este documento identifica os perfis previstos e os requisitos de alto nível do
Ngola Health. Descreve necessidades de produto, não funcionalidades já
entregues. A implementação e a aceitação de cada requisito seguem as tarefas
do [roadmap](ROADMAP.md).

## Perfis de utilizador

Os perfis internos operam no âmbito do hospital a que pertencem. A lista
descreve as suas necessidades gerais; não concede, por si só, permissões a
nenhum endpoint ou dado. A matriz detalhada de perfil × operação deve ser
definida e testada na Fase 2, antes de se considerar o controlo de acesso
concluído.

| Perfil | Necessidades principais |
| --- | --- |
| **Administrador** | Gerir utilizadores e configurações operacionais do hospital, atribuir perfis autorizados e acompanhar a actividade sujeita a auditoria. A distinção entre administrador do hospital e super-administrador da plataforma será definida na ADR-0003. |
| **Gestor** | Consultar informação operacional e relatórios necessários à gestão do hospital, respeitando o âmbito e as permissões atribuídos. |
| **Médico** | Consultar os pacientes sob o seu âmbito de acesso, acompanhar episódios clínicos, registar informação clínica e emitir prescrições conforme as autorizações aplicáveis. |
| **Enfermeiro** | Consultar a informação necessária à prestação de cuidados, participar na triagem e registar observações e actividades de enfermagem conforme as autorizações aplicáveis. |
| **Recepcionista** | Registar e localizar pacientes, gerir informação administrativa de atendimento e apoiar a marcação de consultas, sem acesso automático a informação clínica não necessária às suas funções. |
| **Farmacêutico** | Consultar prescrições autorizadas e gerir medicamentos, lotes, movimentos e níveis de stock da farmácia do seu hospital. |
| **Financeiro** | Gerir e consultar facturas, pagamentos e informação financeira no âmbito autorizado. O acesso a informação clínica não decorre deste perfil. |
| **Técnico de laboratório** | Consultar pedidos de laboratório e registar resultados associados, no âmbito do hospital e das autorizações atribuídas. |
| **Paciente** | Aceder à área destinada aos pacientes para consultar as funcionalidades que venham a ser disponibilizadas ao seu próprio registo, sem acesso às funções internas ou aos dados de outros pacientes. |

### Separação entre paciente e utilizador interno

O paciente é um perfil distinto dos perfis internos. A autenticação, o papel e
o *audience* do portal do paciente devem ser separados dos usados nas funções
internas. Um token do paciente não pode autorizar operações internas, e um
utilizador interno não deve obter acesso ao portal do paciente apenas por
pertencer a um hospital. Esta separação deve ser coberta por testes de
autorização.

## Requisitos funcionais

Os identificadores abaixo permitem referenciar requisitos em tarefas e
decisões futuras. As fases indicam a ordem planeada no roadmap.

| ID | Requisito | Fase |
| --- | --- | --- |
| RF-01 | O sistema deve autenticar utilizadores internos e aplicar permissões às operações protegidas. | 2 |
| RF-02 | O sistema deve gerir hospitais e associar os dados e os utilizadores internos ao âmbito hospitalar aplicável. | 2 |
| RF-03 | O sistema deve isolar automaticamente os dados entre hospitais e impedir que uma conta aceda a dados de outro hospital sem autorização explícita. | 2 |
| RF-04 | O sistema deve gerir utilizadores internos, os seus perfis e o estado das suas contas. | 2 |
| RF-05 | O sistema deve registar acções de escrita para auditoria e permitir a consulta autorizada desses registos. | 2 |
| RF-06 | O sistema deve permitir registar, localizar e actualizar pacientes, detectando possíveis duplicados e validando os identificadores aplicáveis. | 3 |
| RF-07 | O sistema deve permitir abrir, actualizar e fechar episódios clínicos, validando as transições de estado. | 3 |
| RF-08 | O sistema deve apoiar a triagem, apresentando prioridades e tempos-alvo definidos para o fluxo adoptado. | 3 |
| RF-09 | O sistema deve permitir gerir medicamentos, lotes, movimentos de stock e validades, dando prioridade à saída FEFO quando aplicável. | 3 |
| RF-10 | O sistema deve permitir criar prescrições e validar as regras aplicáveis, incluindo a disponibilidade de stock quando necessária. | 3 |
| RF-11 | O sistema deve permitir gerir pedidos de laboratório e os respectivos resultados. | 3 |
| RF-12 | O sistema deve permitir gerir notificações internas relacionadas com os fluxos da aplicação. | 4 |
| RF-13 | O sistema deve permitir configurar horários e disponibilidades e gerir marcações, estados e lembretes de consultas. | 4 |
| RF-14 | O sistema deve permitir gerir enfermarias, camas, admissões, transferências e altas, sem atribuir uma cama ocupada a mais de um paciente. | 4 |
| RF-15 | O sistema deve permitir gerir facturas, itens e pagamentos, mantendo os valores monetários com precisão decimal e a moeda prevista para o projecto. | 5 |
| RF-16 | O sistema deve suportar a integração de facturação electrónica com a AGT após confirmação dos requisitos regulamentares e técnicos vigentes. | 5 |
| RF-17 | O sistema deve apoiar a gestão de turnos, folgas, aprovações e registos de assiduidade. | 6 |
| RF-18 | O sistema deve apoiar a gestão de equipamentos e registos de manutenção. | 6 |
| RF-19 | O sistema deve disponibilizar métricas e relatórios operacionais e financeiros de acordo com as permissões atribuídas. | 6 |
| RF-20 | O sistema deve disponibilizar um portal do paciente com autenticação e autorização isoladas das funções internas. | 7 |
| RF-21 | O sistema poderá disponibilizar capacidades de telemedicina depois de registadas as decisões de arquitectura e os requisitos aplicáveis. | 7 |

## Requisitos transversais

Os requisitos transversais abaixo complementam os requisitos `R1`–`R17` do
[roadmap](ROADMAP.md):

- **Acesso negado por omissão:** só operações explicitamente autorizadas podem
  ser executadas; endpoints de gestão e operação devem ter autorização
  verificada por testes.
- **Isolamento hospitalar:** a separação de dados deve ser aplicada
  automaticamente e testada com contas de hospitais distintos.
- **Autenticação e erros:** a autenticação não deve ocultar erros da aplicação;
  mensagens apresentadas às pessoas devem ser controladas e não revelar
  detalhes internos fora de `dev`.
- **Auditoria:** os registos de auditoria devem ser produzidos por acções
  relevantes e a produção dos registos deve ser provada por teste.
- **Segredos:** segredos, credenciais e dados reais de saúde não podem ser
  incluídos no repositório nem em exemplos, relatórios ou issues.
- **Integridade:** operações concorrentes que afectam stock, slots de consulta
  ou camas devem ser protegidas por restrições e testes.
- **Qualidade:** o CI deve executar os testes; migrações, documentação e
  contratos da API devem acompanhar as alterações correspondentes.

## Questões por decidir

Os pontos seguintes não devem ser inferidos apenas a partir deste documento:

- A distinção, o âmbito e as permissões do administrador da plataforma e do
  administrador de cada hospital (ADR-0003).
- A matriz completa de permissões para cada perfil e endpoint.
- A validação clínica dos tempos-alvo de triagem actualmente configurados e a
  sua adequação ao protocolo adoptado por cada hospital.
- A resolução e eventual fusão de candidatos a pacientes duplicados, a
  correcção auditável de resultados laboratoriais, a validação clínica das
  regras de prescrição e os critérios para encerramento de episódios.
- Os requisitos vigentes da AGT antes da Fase 5.
- O modelo final de telemedicina e os requisitos de privacidade aplicáveis.
