# Agendamento, notificações e internamento

Este guia descreve os fluxos operacionais implementados para gerir horários,
consultas, notificações internas, enfermarias, camas e internamentos. Não é um
protocolo clínico: as decisões e os registos clínicos continuam a ser da
responsabilidade dos profissionais autorizados. As permissões efectivas são
sempre aplicadas pelo backend; os menus e as rotas do frontend não substituem
a autorização da API. Consulte também
[segurança, perfis e isolamento multi-hospital](security.md).

## Âmbito e navegação

Os registos pertencem ao hospital activo da sessão. A interface está disponível
nas rotas internas:

| Fluxo | Rota |
| --- | --- |
| Consultas agendadas | `/scheduling` |
| Calendário | `/scheduling/calendar` |
| Nova consulta | `/scheduling/new` |
| Horários dos médicos e bloqueios | `/scheduling/schedules` |
| Detalhe de uma consulta | `/scheduling/:id` |
| Mapa de camas | `/inpatient` |
| Lista de internamentos | `/inpatient/admissions` |
| Nova admissão | `/inpatient/admissions/new` |
| Detalhe, transferência e alta | `/inpatient/admissions/:id` |
| Configuração de enfermarias e camas | `/inpatient/setup` |
| Lista paginada de notificações | `/notifications` |

As consultas de disponibilidade, calendário e mapa mostram dados operacionais;
não confirmam a adequação clínica da consulta ou da admissão.

## Agendamento

### Horários, capacidade e bloqueios

Um horário define o médico, o dia da semana, o intervalo de funcionamento, a
duração de cada slot e o número máximo de pacientes por slot. Os dias são
representados de segunda-feira (`0`) a domingo (`6`). Se não forem fornecidos,
a duração assume 30 minutos e a capacidade assume um paciente. O início tem de
ser anterior ao fim; a duração do slot não pode exceder o intervalo definido e
horários activos sobrepostos para o mesmo médico são recusados.

Um bloqueio pode abranger o dia inteiro ou um intervalo. Bloqueios parciais
exigem horas válidas de início e fim; um slot que intersecte um bloqueio não é
apresentado como disponível. Os horários são desactivados, não apagados, quando
são removidos.

A disponibilidade é calculada a partir do horário activo, dos bloqueios e das
marcações existentes. Slots em datas anteriores, bloqueados ou sem capacidade
livre não estão disponíveis. Consultas canceladas e faltas (`NO_SHOW`) deixam
de ocupar capacidade; consultas agendadas, confirmadas e realizadas continuam
a contar. A consulta de disponibilidade é informativa: ao criar a marcação, o
backend volta a validar o slot e a capacidade sob concorrência. Uma colisão ou
um slot já preenchido é recusado com conflito; a interface deve pedir ao
utilizador que escolha outro horário.

### Estados das consultas

Uma nova marcação começa em `SCHEDULED`. As transições permitidas são:

| Acção | Estado de origem | Estado resultante |
| --- | --- | --- |
| Confirmar | `SCHEDULED` | `CONFIRMED` |
| Marcar como realizada | `SCHEDULED` ou `CONFIRMED` | `COMPLETED` |
| Registar falta | `SCHEDULED` ou `CONFIRMED` | `NO_SHOW` |
| Cancelar | Qualquer estado excepto `COMPLETED` e `NO_SHOW` | `CANCELLED` |

O motivo é obrigatório na criação. A marcação referencia um paciente, médico,
data e hora de início que correspondam a um slot válido; o tipo assume
`OUTPATIENT` quando não é indicado. Um paciente não pode ter duas marcações
activas no mesmo slot do mesmo médico. Não existe nesta API uma operação de
reagendamento: para alterar a hora, é necessário cancelar e criar outra
marcação.

### API e perfis

O prefixo dos endpoints é `/api/scheduling`.

| Operação | Endpoint | Acesso |
| --- | --- | --- |
| Consultar horários do hospital ou de um médico | `GET /schedules`, `GET /schedules/doctor/{doctorId}` | Utilizador autenticado |
| Criar ou desactivar horário | `POST /schedules`, `DELETE /schedules/{id}` | `ADMIN`, `MANAGER` |
| Consultar disponibilidade diária | `GET /availability?doctorId=…&date=AAAA-MM-DD` | Utilizador autenticado |
| Consultar calendário por intervalo | `GET /calendar?from=AAAA-MM-DD&to=AAAA-MM-DD` | Utilizador autenticado |
| Listar marcações, com filtros e paginação | `GET /appointments` | Utilizador autenticado |
| Consultar detalhe | `GET /appointments/{id}` | Utilizador autenticado |
| Criar marcação | `POST /appointments` | `ADMIN`, `DOCTOR`, `RECEPTIONIST`, `MANAGER` |
| Confirmar, cancelar ou registar falta | `PATCH /appointments/{id}/confirm`, `/cancel`, `/no-show` | `ADMIN`, `DOCTOR`, `RECEPTIONIST`, `MANAGER` |
| Marcar consulta como realizada | `PATCH /appointments/{id}/complete` | `ADMIN`, `DOCTOR`, `MANAGER` |
| Bloquear horário | `POST /blocks` | `ADMIN`, `MANAGER` |

O cancelamento recebe o motivo no parâmetro `reason`. Os filtros de listagem
aceitam médico, paciente, estado e data; as datas usam o formato ISO `AAAA-MM-DD`.
Todos os endpoints continuam sujeitos ao âmbito do hospital activo.

## Notificações

As notificações são registos internos consultados pelo utilizador autenticado
no seu hospital. Uma notificação pode ser dirigida a um utilizador específico
ou, quando criada sem destinatário, ficar visível aos utilizadores desse
hospital. A leitura e a marcação como lida são individuais por utilizador. A
API permite listar notificações por página (20 por omissão, até 100), consultar
o total por ler, obter as cinco mais recentes por ler e marcar uma ou todas
como lidas.

O sino do frontend actualiza a contagem por consulta periódica, de 60 em 60
segundos, e abre a lista das cinco notificações mais recentes por ler. A página
`/notifications` apresenta o histórico paginado. Uma ligação de acção pode
navegar para o registo relacionado; a autorização desse destino continua a ser
verificada pela API correspondente.

Eventos actualmente documentados no código:

- O cancelamento de uma consulta gera uma notificação de prioridade alta para o
  médico responsável.
- Às 18:00 no fuso `Africa/Luanda`, o processo agendado procura consultas do
  dia seguinte nos estados `SCHEDULED` e `CONFIRMED` e cria um lembrete de
  prioridade média para o médico.
- A disponibilização de resultados laboratoriais gera uma notificação de
  prioridade alta para o utilizador destinatário do pedido.
- As verificações diárias de stock e facturas também podem criar notificações
  hospitalares, conforme as regras desses módulos.

Os eventos de cancelamento de consulta e de resultado laboratorial são tratados
por listeners Spring associados à transacção. O processamento é interno à
aplicação: não há broker, envio de SMS ou correio electrónico neste fluxo.
Notificações com a mesma referência e tipo são deduplicadas durante 24 horas.
Este mecanismo não substitui a confirmação com o paciente nem garante que ele
receba um aviso.

### API de notificações

O prefixo é `/api/notifications`; todos os endpoints exigem autenticação
interna e devolvem apenas notificações visíveis ao utilizador e hospital da
sessão.

| Operação | Endpoint |
| --- | --- |
| Listar notificações paginadas | `GET /` |
| Contar notificações por ler | `GET /unread-count` |
| Obter até cinco não lidas | `GET /top-unread` |
| Marcar uma como lida | `PATCH /{id}/read` |
| Marcar todas como lidas | `PATCH /read-all` |

Uma notificação de outro utilizador ou hospital não pode ser marcada como lida
através do seu identificador.

## Enfermarias, camas e internamentos

### Fluxo operacional

1. Um utilizador autorizado configura uma enfermaria e adiciona camas.
2. A equipa consulta o mapa por enfermaria. Os estados de cama são
   `AVAILABLE`, `RESERVED`, `MAINTENANCE` e `OCCUPIED`.
3. Uma admissão selecciona paciente, médico responsável e cama `AVAILABLE` ou
   `RESERVED`; a data prevista de alta é opcional. O internamento começa em
   `ACTIVE` e a cama passa a `OCCUPIED`.
4. Uma transferência selecciona uma cama disponível ou reservada, na mesma ou
   noutra enfermaria. A cama de origem fica `AVAILABLE`, a cama de destino
   passa a `OCCUPIED` e o histórico regista origem, destino, utilizador e
   motivo.
5. Uma alta regista a condição e notas opcionais, liberta a cama e conclui o
   internamento. A condição `DECEASED` atribui o estado `DECEASED`; as outras
   condições atribuem `DISCHARGED`.

O estado `OCCUPIED` só é alterado pelo fluxo de admissão, transferência ou
alta; não deve ser definido manualmente no mapa nem na configuração. A base de
dados e os bloqueios transaccionais protegem contra duas admissões activas para
a mesma cama ou para o mesmo paciente no hospital. Se a cama ou o internamento
forem alterados em simultâneo, a operação concorrente inválida é recusada e a
interface deve voltar a carregar os dados.

A transferência mantém o internamento activo e acrescenta uma entrada ao
histórico; não muda o estado do internamento para `TRANSFERRED`. A lista
permite consultar internamentos por estado e enfermaria. A interface de
admissão aberta a partir do mapa recebe a enfermaria e cama escolhidas, mas
volta a validar a disponibilidade no backend quando a admissão é submetida.

### API e perfis

O prefixo é `/api/inpatient`.

| Operação | Endpoint | Acesso |
| --- | --- | --- |
| Consultar enfermarias, camas, mapa ou internamentos | `GET /wards`, `GET /wards/{wardId}/beds`, `GET /wards/{wardId}/map`, `GET /admissions`, `GET /admissions/active`, `GET /admissions/{id}` | Utilizador autenticado |
| Criar enfermaria ou cama | `POST /wards`, `POST /beds` | `ADMIN`, `MANAGER` |
| Alterar estado não ocupado da cama | `PATCH /beds/{id}/status` | `ADMIN`, `MANAGER`, `NURSE` |
| Admitir paciente | `POST /admissions` | `ADMIN`, `DOCTOR`, `NURSE`, `MANAGER`, `RECEPTIONIST` |
| Transferir paciente | `PATCH /admissions/{id}/transfer` | `ADMIN`, `DOCTOR`, `NURSE`, `MANAGER` |
| Dar alta | `PATCH /admissions/{id}/discharge` | `ADMIN`, `DOCTOR`, `MANAGER` |

Os endpoints de leitura também são isolados pelo hospital da sessão. A leitura
permitida a um perfil não implica permissão para criar, transferir ou dar alta.

## Relação com o fluxo clínico

Uma marcação não cria automaticamente um episódio clínico, triagem, admissão
ou lembrete externo. Uma admissão também não cria automaticamente um episódio.
Os módulos mantêm estados próprios e a interface não substitui a confirmação
de identidade, a avaliação clínica, o consentimento ou os procedimentos
internos de cada hospital. Para o restante do percurso clínico, consulte o
[guia de fluxo clínico](clinical-workflow.md).

## Referências de implementação

| Área | Código de referência |
| --- | --- |
| Horários, disponibilidade e estados de consulta | [`SchedulingController`](../backend/src/main/java/ao/hospitalao/modules/scheduling/controller/SchedulingController.java) e [`SchedulingService`](../backend/src/main/java/ao/hospitalao/modules/scheduling/service/SchedulingService.java) |
| Notificações, consultas por utilizador e agendamento dos lembretes | [`NotificationController`](../backend/src/main/java/ao/hospitalao/modules/notifications/controller/NotificationController.java) e [`NotificationService`](../backend/src/main/java/ao/hospitalao/modules/notifications/service/NotificationService.java) |
| Eventos transaccionais de consulta e laboratório | [`NotificationEventListener`](../backend/src/main/java/ao/hospitalao/modules/notifications/service/NotificationEventListener.java) |
| Enfermarias, camas, admissões, transferências e altas | [`InpatientController`](../backend/src/main/java/ao/hospitalao/modules/inpatient/controller/InpatientController.java), [`AdmissionService`](../backend/src/main/java/ao/hospitalao/modules/inpatient/service/AdmissionService.java), [`BedTransferService`](../backend/src/main/java/ao/hospitalao/modules/inpatient/service/BedTransferService.java) e [`DischargeService`](../backend/src/main/java/ao/hospitalao/modules/inpatient/service/DischargeService.java) |
