# Visão, escopo e glossário

## Visão

O Ngola Health pretende ser uma plataforma de gestão de saúde para instituições
em Angola, com suporte a vários hospitais e aos seus processos clínicos e
administrativos. A visão é oferecer uma base comum para gerir informação e
operações, preservando a confidencialidade dos dados e separando o acesso de
cada hospital.

O sistema deve apoiar os profissionais e os serviços administrativos, sem
substituir o julgamento clínico nem as responsabilidades das instituições de
saúde.

## Objectivos do projecto

- Organizar informação de pacientes e os respectivos episódios de atendimento.
- Apoiar fluxos de triagem, prescrição, farmácia e laboratório.
- Apoiar agendamentos, internamentos, facturação e operações administrativas.
- Separar os dados e o acesso por hospital, com controlo de permissões e
  auditoria.
- Disponibilizar, numa fase posterior, um portal destinado aos pacientes e
  capacidades de telemedicina.
- Desenvolver cada capacidade com testes, documentação e requisitos de
  segurança desde o início.

Estes objectivos descrevem o âmbito planeado. Não afirmam que os módulos estejam
implementados, validados clinicamente ou prontos para utilização em produção.
O estado de cada entrega é acompanhado pelas tarefas e critérios de conclusão
do [roadmap](ROADMAP.md).

## Escopo planeado

O desenvolvimento está organizado por fases:

1. **Fundação:** repositório, documentação, estrutura técnica, testes e
   integração contínua.
2. **Identidade e acesso:** hospitais, utilizadores, autenticação, permissões,
   auditoria e isolamento multi-hospital.
3. **Núcleo clínico:** pacientes, episódios, triagem, farmácia, prescrições e
   laboratório.
4. **Operações de atendimento:** notificações, agendamento e internamento.
5. **Operações financeiras:** facturação e integração com a AGT, sujeita à
   confirmação dos requisitos vigentes antes do início dessa fase.
6. **Gestão e operações:** recursos humanos, equipamentos, dashboards e
   relatórios.
7. **Serviços para pacientes:** portal do paciente e telemedicina, sujeitos às
   decisões de arquitectura previstas no roadmap.

O detalhe, a ordem e os critérios de aceitação de cada entrega são os do
[roadmap](ROADMAP.md). Uma fase planeada não deve ser entendida como
funcionalidade disponível.

## Fora do escopo inicial

- Substituir o julgamento dos profissionais de saúde ou tomar decisões
  clínicas autónomas.
- Integrar serviços de infraestrutura sem um caso de uso concreto e uma
  decisão de arquitectura registada. Em particular, Redis, RabbitMQ, MinIO,
  SMTP e WebSocket não são requisitos automáticos da fundação.
- Assumir conformidade regulamentar ou integração com serviços externos antes
  de os requisitos aplicáveis serem confirmados.
- Tratar funcionalidades ainda não concluídas no roadmap como disponíveis
  para utilização real.

Qualquer alteração significativa ao escopo deve ser registada numa tarefa ou
decisão de arquitectura, de acordo com as convenções do projecto.

## Glossário

| Termo | Significado no projecto |
| --- | --- |
| **Hospital** | Instituição de saúde representada no sistema e usada como fronteira de isolamento dos seus dados. |
| **Multi-hospital** | Modelo em que várias instituições usam a plataforma, sem que os utilizadores de uma instituição acedam aos dados de outra sem autorização. |
| **Utilizador interno** | Pessoa com uma conta para aceder às funções profissionais ou administrativas do sistema. É distinto de um paciente, mesmo quando a mesma pessoa possa ter ambos os papéis fora do sistema. |
| **Paciente** | Pessoa que recebe atendimento de saúde e a quem pertencem os dados clínicos registados. |
| **Episódio** | Registo de um atendimento ou percurso clínico associado a um paciente, com estado e informação clínica próprios. |
| **Triagem** | Avaliação inicial que organiza a prioridade e o tempo-alvo de atendimento. Não equivale a diagnóstico. |
| **Internamento** | Período em que um paciente admitido permanece sob cuidados da instituição, incluindo a atribuição e eventual transferência de cama. |
| **Agendamento** | Marcação de uma consulta ou atendimento num horário ou slot disponível. |
| **Prescrição** | Registo de uma orientação terapêutica emitida por um profissional autorizado, incluindo medicamentos quando aplicável. |
| **Farmácia** | Área de gestão de medicamentos, lotes, movimentos e disponibilidade de stock. |
| **FEFO** | *First Expired, First Out*: regra de saída que dá prioridade ao lote cuja validade termina primeiro. |
| **Auditoria** | Registo rastreável de acções efectuadas no sistema, incluindo o actor e a operação. |
| **Portal do paciente** | Área de acesso destinada ao paciente, separada das funções e credenciais dos utilizadores internos. |
| **Perfil** | Conjunto de permissões associado a uma conta, que determina as operações autorizadas. |
| **Isolamento por hospital** | Aplicação automática das regras que impedem a exposição de dados de outro hospital. |

## Convenções linguísticas

A documentação e os textos apresentados às pessoas são escritos em português.
Os identificadores do domínio, o código, as tabelas, colunas, rotas, DTOs e
SQL são escritos em inglês. A documentação do projecto segue a grafia
portuguesa anterior ao Acordo Ortográfico de 1990, conforme as convenções
registadas no [roadmap](ROADMAP.md).
