# Fluxo clínico e regras de negócio

Este guia descreve o comportamento implementado no núcleo clínico do Ngola
Health e uma sequência operacional recomendada. Não é um protocolo médico, não
substitui o julgamento dos profissionais e não afirma que o sistema imponha
automaticamente todas as etapas descritas.

## Princípios e limites

- Os registos clínicos pertencem a um hospital. O backend aplica o âmbito
  hospitalar e as permissões por operação; o menu do frontend não concede
  autorização. Consulte [segurança e perfis](security.md).
- Um paciente é distinto de um utilizador interno. O cadastro do paciente não
  cria, por si só, credenciais de acesso ao sistema.
- A sequência abaixo é uma orientação de utilização. Actualmente, triagem,
  episódios, pedidos de laboratório e prescrições são operações separadas;
  criar um registo não inicia automaticamente o seguinte.
- O sistema organiza registos e estados. Não diagnostica, não escolhe
  tratamentos e não confirma que um resultado ou uma prioridade sejam
  clinicamente correctos.
- Os tempos-alvo de triagem descritos abaixo são os valores configurados no
  código. A sua aprovação e adequação ao protocolo clínico de cada hospital
  continuam sujeitas a validação profissional.

## Sequência operacional recomendada

### 1. Localizar ou registar o paciente

Antes de criar um registo, procure o paciente no hospital activo. O sistema
permite consultar até dez candidatos a possível duplicado quando coincidem a
data de nascimento e o nome completo (ignorando maiúsculas/minúsculas e
espaços exteriores) ou o telefone.

Esta pesquisa é um apoio à conferência, não uma decisão automática de
identidade: não funde registos nem impede, por si só, a criação de um paciente
com dados demográficos semelhantes. Dentro de cada hospital, o BI/NIF e o
número do cartão de saúde, quando informados, têm de ser únicos. Os valores
são normalizados antes da verificação; uma colisão impede a criação ou
actualização.

Um paciente pode ser actualizado ou desactivado. A desactivação preserva o
registo; não equivale a apagar o historial clínico.

### 2. Fazer triagem quando o atendimento o exigir

A triagem regista a queixa principal, sinais vitais e prioridade. É possível
associar um paciente registado ou, em situação de urgência, informar um nome
temporário. A queixa principal é obrigatória. Sem prioridade indicada, o
sistema atribui `GREEN` (*Pouco Urgente*).

| Prioridade | Designação apresentada | Tempo-alvo configurado |
| --- | --- | ---: |
| `RED` | Imediato | 0 minutos |
| `ORANGE` | Muito urgente | 10 minutos |
| `YELLOW` | Urgente | 60 minutos |
| `GREEN` | Pouco urgente | 120 minutos |
| `BLUE` | Não urgente | 240 minutos |

Um registo em espera é assinalado como fora do tempo-alvo quando o tempo de
espera ultrapassa o valor configurado. A prioridade só pode ser alterada
enquanto o registo estiver em espera. O fluxo de triagem passa de `WAITING`
para `IN_PROGRESS` e, daí, para `COMPLETED`; um paciente ainda em espera também
pode ser marcado como `LEFT`.

Uma triagem não é um diagnóstico nem cria automaticamente um episódio. O
registo de triagem e o episódio permanecem entidades separadas.

### 3. Abrir e acompanhar um episódio

Um episódio pertence a um paciente e pode ser do tipo urgência, ambulatório,
internamento, cirurgia ambulatória ou exame. É criado no estado `SCHEDULED`.
O profissional pode iniciar o episódio (`IN_PROGRESS`) e concluí-lo
(`COMPLETED`); a conclusão só é aceite depois do início. Um episódio não
concluído pode ser cancelado.

O episódio contém informação clínica e sinais vitais. A sua criação não
depende de existir uma triagem e não altera automaticamente o estado de uma
triagem. Se for necessário associar a triagem ao episódio, essa relação deve
ser registada pelo fluxo que a disponibilizar; não se deve presumir uma
associação automática.

### 4. Pedir exames e registar resultados

Um pedido de laboratório requer um paciente e pelo menos um exame activo do
catálogo. O mesmo exame não pode ser repetido no pedido. O pedido pode ser
associado a um episódio, mas o episódio tem de pertencer ao mesmo paciente.
Quando não é indicado outro profissional solicitante, fica registado o
utilizador autenticado.

O percurso do pedido é:

`PENDING` → `COLLECTED` → `IN_ANALYSIS` → `COMPLETED`

- Só pedidos `PENDING` podem avançar para recolha.
- Só pedidos `COLLECTED` podem iniciar análise.
- Os resultados são registados por item enquanto o pedido está em análise.
- O pedido é concluído automaticamente quando todos os itens têm resultado.
- Um pedido concluído ou já cancelado não pode ser cancelado novamente.
- O fluxo actual não permite substituir um resultado já registado. Uma
  correcção clínica requer um processo explícito, a definir antes de ser
  acrescentado ao sistema.

O registo de ficheiros anexos não faz parte deste fluxo.

### 5. Emitir prescrição e dispensar medicamentos

Uma prescrição pode estar associada a um episódio ou a um internamento, mas
nunca a ambos. O paciente associado tem de ser o mesmo do episódio ou do
internamento. A prescrição deve conter pelo menos um medicamento activo e a
quantidade solicitada tem de estar disponível no stock elegível.

Se não for indicada uma validade, o sistema usa 30 dias; o valor aceite é de 1
a 365 dias. A data é calculada no fuso `Africa/Luanda`. Na dispensa, a farmácia
selecciona os lotes pela regra FEFO (*First Expired, First Out*), regista as
quantidades e actualiza o estado da prescrição e dos respectivos itens.

Uma prescrição cancelada ou expirada não deve ser dispensada. O perfil do
utilizador determina quem pode emitir, dispensar ou cancelar; consulte a
[matriz de segurança](security.md) e as regras dos endpoints.

### 6. Concluir o episódio

Depois de rever a informação clínica e os resultados relevantes, o
profissional pode concluir um episódio que esteja `IN_PROGRESS`. A conclusão
não fecha, cancela nem valida automaticamente pedidos de laboratório,
prescrições ou dispensas. Esses registos mantêm o seu próprio estado e têm de
ser acompanhados separadamente.

## Regras transversais

1. **Âmbito hospitalar:** pacientes, episódios, triagens, pedidos, exames,
   prescrições e stock são consultados no âmbito do hospital activo.
2. **Identidade:** a correspondência de possíveis duplicados não substitui a
   confirmação humana; identificadores únicos são verificados por hospital.
3. **Transições:** cada módulo mantém o seu próprio estado. Uma operação
   inválida é recusada pelo backend, mesmo que seja tentada directamente fora
   da interface.
4. **Permissões:** leitura e escrita variam por perfil e por operação. Não
   inferir autorização a partir do acesso a outro ecrã do módulo.
5. **Sem automatização clínica:** prioridades, diagnósticos, prescrições e
   resultados são introduzidos ou confirmados por profissionais autorizados.

## Decisões e limitações a acompanhar

- A direcção clínica de cada hospital deve validar os tempos-alvo de triagem e
  decidir se correspondem ao protocolo local.
- O sistema não dispõe de um processo de fusão de pacientes duplicados nem de
  correcção auditável de resultados laboratoriais já submetidos.
- A triagem não é ligada automaticamente a um episódio; a criação de pedidos
  de exame e de prescrições também não é automática.
- O agendamento, as notificações internas e o internamento têm fluxos
  operacionais próprios, descritos no guia de
  [agendamento, notificações e internamento](scheduling-notifications-inpatient.md).
  Não são passos automáticos da sequência clínica acima. O portal do paciente
  continua a seguir o [roadmap](ROADMAP.md).

## Referências de implementação

| Área | Código de referência |
| --- | --- |
| Cadastro, pesquisa de duplicados e identificadores únicos | [`PatientService`](../backend/src/main/java/ao/hospitalao/modules/patients/service/PatientService.java) e [`PatientRepository`](../backend/src/main/java/ao/hospitalao/modules/patients/repository/PatientRepository.java) |
| Estados e tipos de episódio | [`EpisodeService`](../backend/src/main/java/ao/hospitalao/modules/episodes/service/EpisodeService.java) e [`Episode`](../backend/src/main/java/ao/hospitalao/modules/episodes/entity/Episode.java) |
| Prioridade e tempos-alvo de triagem | [`TriageRecord`](../backend/src/main/java/ao/hospitalao/modules/triage/entity/TriageRecord.java) e [`TriageService`](../backend/src/main/java/ao/hospitalao/modules/triage/service/TriageService.java) |
| Pedidos e resultados laboratoriais | [`LabService`](../backend/src/main/java/ao/hospitalao/modules/laboratory/service/LabService.java) |
| Validade, stock e dispensa de prescrições | [`PrescriptionService`](../backend/src/main/java/ao/hospitalao/modules/prescription/service/PrescriptionService.java) |
