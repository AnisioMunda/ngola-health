# Gestão e operações

Este guia descreve os ecrãs e endpoints da Fase 6: recursos humanos,
equipamentos, dashboards e relatórios. A API usa o prefixo `/api` e exige
autenticação Bearer; o hospital activo do utilizador determina o âmbito dos
dados.

## Recursos humanos

Os ecrãs estão disponíveis em `/hr`, `/hr/shifts`, `/hr/leaves` e
`/hr/attendance`. O painel agrega presenças, ausências, turnos do dia e pedidos
de folga pendentes.

| Método e endpoint | Acesso | Utilização |
| --- | --- | --- |
| `GET /api/hr/stats` | ADMIN, MANAGER | Indicadores e listas do dia |
| `GET /api/hr/shifts/weekly?weekStart=AAAA-MM-DD` | Autenticado | Escala de sete dias; `weekStart` é opcional |
| `GET /api/hr/shifts/my?from=AAAA-MM-DD&to=AAAA-MM-DD` | Autenticado | Turnos do utilizador autenticado |
| `POST /api/hr/shifts` | ADMIN, MANAGER | Criar turno |
| `PATCH /api/hr/shifts/{id}/status` | ADMIN, MANAGER | Actualizar estado e notas |
| `DELETE /api/hr/shifts/{id}` | ADMIN, MANAGER | Eliminar turno não concluído |
| `GET /api/hr/leaves/pending` | ADMIN, MANAGER | Pedidos por decidir |
| `GET /api/hr/leaves/my?page=0&size=20` | Autenticado | Pedidos do utilizador autenticado |
| `GET /api/hr/leaves/approved?from=AAAA-MM-DD&to=AAAA-MM-DD` | ADMIN, MANAGER | Ausências aprovadas no período |
| `POST /api/hr/leaves` | Autenticado | Submeter pedido de folga |
| `PATCH /api/hr/leaves/{id}/approve` | ADMIN, MANAGER | Aprovar ou rejeitar pedido |
| `PATCH /api/hr/leaves/{id}/cancel` | Autenticado | Cancelar pedido |
| `POST /api/hr/attendance/check-in` | Autenticado | Registar entrada |
| `PATCH /api/hr/attendance/check-out` | Autenticado | Registar saída e calcular horas |
| `GET /api/hr/attendance/my?from=AAAA-MM-DD&to=AAAA-MM-DD` | Autenticado | Histórico do próprio utilizador |
| `GET /api/hr/attendance/daily?date=AAAA-MM-DD` | ADMIN, MANAGER | Presenças do dia; a data é opcional |

Os tipos de turno são `MORNING`, `AFTERNOON`, `NIGHT`, `FULL_DAY` e
`ON_CALL`. Os pedidos de folga começam em `PENDING`; os estados seguintes são
`APPROVED`, `REJECTED` e `CANCELLED`. Cada utilizador só pode registar um
check-in por dia.

## Equipamentos

O ecrã está disponível em `/equipment`. O catálogo suporta pesquisa e
paginação, estado, localização, garantia, manutenção e calibração.

| Método e endpoint | Acesso | Utilização |
| --- | --- | --- |
| `GET /api/equipment?q=texto&page=0&size=20` | Autenticado | Pesquisar equipamentos |
| `GET /api/equipment/{id}` | Autenticado | Consultar equipamento e histórico |
| `GET /api/equipment/stats` | ADMIN, MANAGER | Indicadores de estado, manutenção e garantia |
| `GET /api/equipment/maintenance-due` | ADMIN, MANAGER | Manutenções vencidas ou nos próximos sete dias |
| `GET /api/equipment/calibration-due` | ADMIN, MANAGER | Calibrações vencidas ou nos próximos sete dias |
| `POST /api/equipment` | ADMIN, MANAGER | Registar equipamento |
| `PATCH /api/equipment/{id}/status` | ADMIN, MANAGER | Alterar estado |
| `POST /api/equipment/{id}/maintenance` | ADMIN, MANAGER | Registar manutenção ou calibração |

Registar manutenção actualiza a próxima data quando fornecida ou calcula-a
com o intervalo configurado. Uma manutenção preventiva pode devolver um
equipamento que esteja em `MAINTENANCE` ao estado `ACTIVE`. A verificação
diária de manutenção corre às 07:00 e, nesta versão, escreve alertas nos
registos do backend; não envia notificações externas.

## Dashboards e relatórios

Os dashboards e relatórios pertencem ao hospital activo. O dashboard geral
está em `/dashboard`; o dashboard de indicadores avançados também expõe
`GET /api/dashboard/advanced`. Ambos exigem autenticação.

O ecrã de relatórios está em `/reports` e o dashboard executivo em
`/reports/executive`.

| Método e endpoint | Acesso | Utilização |
| --- | --- | --- |
| `GET /api/reports/advanced/executive` | ADMIN, MANAGER | Indicadores clínicos, financeiros e operacionais, com tendências |
| `GET /api/reports/advanced/bed-occupancy` | ADMIN, MANAGER | Ocupação e permanência por enfermaria |
| `GET /api/reports/advanced/financial?from=AAAA-MM-DD&to=AAAA-MM-DD` | ADMIN, MANAGER | Totais e tendências financeiras; datas opcionais |
| `GET /api/reports/patients/{id}` | ADMIN, DOCTOR, NURSE, MANAGER | Ficha clínica em PDF |
| `GET /api/reports/stock` | ADMIN, PHARMACIST, MANAGER | Inventário de stock em PDF |

Os endpoints de relatório devolvem dados ou ficheiros PDF; não alteram
registos financeiros ou clínicos.

## Portal do paciente

O portal está disponível em `/portal/login` e `/portal`. O registo é feito
com o número de processo ou NIF e fica pendente até a recepção, um gestor ou
um administrador confirmar presencialmente a identidade do paciente. Só
depois da aprovação é possível iniciar sessão. O token do portal tem âmbito
próprio e não autentica chamadas à API interna.

| Método e endpoint | Acesso | Utilização |
| --- | --- | --- |
| `POST /api/portal/register` | Público | Solicitar acesso; requer email, palavra-passe com pelo menos 12 caracteres e número de processo ou NIF |
| `POST /api/portal/login` | Público | Obter token do portal após aprovação |
| `GET /api/portal/dashboard` | Token do portal | Resumo e próximas consultas do próprio paciente |
| `GET /api/portal/episodes` | Token do portal | Até 50 episódios recentes do próprio paciente |
| `GET /api/portal/lab-results` | Token do portal | Resultados laboratoriais concluídos do próprio paciente |
| `GET /api/portal/prescriptions` | Token do portal | Até 50 prescrições do próprio paciente |
| `GET /api/portal/invoices` | Token do portal | Facturas visíveis do próprio paciente |
| `GET /api/patient-portal/accounts/pending` | ADMIN, MANAGER, RECEPTIONIST | Pedidos de aprovação do hospital activo |
| `PATCH /api/patient-portal/accounts/{accountId}/approve` | ADMIN, MANAGER, RECEPTIONIST | Confirmar identidade e activar conta no hospital activo |

O âmbito do paciente é obtido do token, nunca de um identificador de paciente
enviado pelo browser. O portal é de consulta nesta versão; não agenda nem
altera dados clínicos.

## Telemedicina com Microsoft Teams

O ecrã operacional está em `/telemedicine`. A integração cria reuniões
independentes do calendário através do Microsoft Graph; não há transmissão de
áudio/vídeo pela aplicação. Consulte a [ADR-0012](adr/0012-telemedicine.md)
antes de activar o fornecedor.

Para activar a integração:

1. Registe uma aplicação Microsoft Entra e conceda consentimento de
   administrador para a permissão de aplicação
   `OnlineMeetings.ReadWrite.All`.
2. Crie uma application access policy limitada aos organizadores autorizados
   e atribua-a às contas Entra dos médicos. Cada médico organizador tem de ter
   a licença e a configuração Teams exigidas pela Microsoft.
3. Defina `TEAMS_ENABLED=true`, `TEAMS_TENANT_ID`, `TEAMS_CLIENT_ID` e
   `TEAMS_CLIENT_SECRET` no gestor de segredos do ambiente. Nunca grave o
   segredo no repositório ou na base de dados. Com a integração activa, a
   aplicação recusa iniciar se as credenciais estiverem incompletas.
4. No perfil de cada médico, guarde o object ID do utilizador Microsoft Entra
   no campo de organizador Teams. O campo só pode ser atribuído a utilizadores
   com o papel `DOCTOR`. A lista de médicos da telemedicina inclui apenas
   médicos activos, configurados e pertencentes ao hospital activo.

| Método e endpoint | Acesso | Utilização |
| --- | --- | --- |
| `GET /api/telemedicine/stats` | ADMIN, MANAGER, DOCTOR | Indicadores do hospital activo e estado da integração |
| `GET /api/telemedicine/active` | ADMIN, MANAGER, DOCTOR | Sessões activas do hospital activo |
| `GET /api/telemedicine/my-sessions` | DOCTOR | Sessões do médico autenticado no dia |
| `GET /api/telemedicine/doctors` | ADMIN, DOCTOR, MANAGER, RECEPTIONIST | Médicos elegíveis do hospital activo |
| `POST /api/telemedicine` | ADMIN, DOCTOR, MANAGER, RECEPTIONIST | Criar sessão Teams para paciente e médico do mesmo hospital |
| `PATCH /api/telemedicine/room/{token}/join` | DOCTOR, ADMIN | Iniciar sessão |
| `PATCH /api/telemedicine/{id}/end` | DOCTOR, ADMIN | Concluir sessão e guardar notas clínicas |
| `PATCH /api/telemedicine/{id}/cancel` | ADMIN, DOCTOR, MANAGER, RECEPTIONIST | Cancelar sessão |

A criação requer `patientId`, `doctorId`, `scheduledAt` futuro e
`durationMinutes` entre 1 e 1440; `appointmentId` é opcional e, se indicado,
tem de corresponder ao mesmo paciente, médico e hospital. O assunto da reunião
é sempre genérico ("Consulta médica"), sem dados do paciente. A aplicação
guarda o ID da reunião e o organizador para poder revogar a ligação quando a
sessão termina ou é cancelada. A URL é removida da resposta depois da
revogação. Se o Graph não confirmar a revogação, a operação falha e o estado
local não é alterado.

As ligações Teams são sensíveis: não as copie para logs, mensagens ou canais
públicos. Confirme as permissões de acesso aos endpoints antes de partilhar
uma ligação com participantes. Rever requisitos de privacidade, retenção e
localização de dados do fornecedor é obrigatório antes da disponibilização em
produção.

## Persistência e validação

As tabelas de RH e equipamentos são criadas pelas migrações Liquibase
`human_resources/001-human-resources.sql` e `equipment/001-equipment.sql`.
Use as migrações do changelog principal; não edite changesets já aplicados.

Para validar os serviços e a fronteira modular do backend:

```sh
cd backend
mvn -Djacoco.skip=true \
  -Dtest=ModuleArchitectureTest,ShiftServiceTest,LeaveServiceTest,AttendanceServiceTest,EquipmentServiceTest \
  test
```

Os testes de integração que usam Testcontainers precisam de Docker activo.

## Operação local

O ambiente local é iniciado com `docker compose up --build --detach` conforme
o [guia de desenvolvimento](development.md). Confirme os estados dos serviços
e acompanhe os registos:

```sh
docker compose ps
docker compose logs --since=30m backend
docker compose logs --since=30m frontend
```

O backend local está disponível em `http://localhost:8080`; o frontend em
`http://localhost:4200`. Um administrador pode consultar a saúde e as métricas
Actuator localmente:

```sh
curl -fsS -H "Authorization: Bearer $ADMIN_TOKEN" http://localhost:8080/api/actuator/health
curl -fsS -H "Authorization: Bearer $ADMIN_TOKEN" http://localhost:8080/api/actuator/metrics
curl -fsS -H "Authorization: Bearer $ADMIN_TOKEN" http://localhost:8080/api/actuator/prometheus
```

Actuator exige papel administrativo; não exponha essas portas numa rede
pública. O Compose de desenvolvimento liga os serviços às interfaces locais.
Para TLS público, renovação Let’s Encrypt e deployment, use o Compose de
produção opcional descrito em
[development.md](development.md#compose-de-produção-opcional).

## Backups e recuperação locais

O perfil opcional `backup` grava dumps PostgreSQL cifrados com `age` no
directório local `backups/`, sem dependência de S3. Pode executar uma cópia
imediata ou activar o scheduler diário às 02:00 UTC; os comandos de preparação
da chave, backup e restauro isolado estão em
[development.md — backups locais](development.md#backups-locais).

Confirme a existência e a data do ficheiro `.dump.age` e teste periodicamente
o restauro numa base local vazia, diferente da origem. Guarde a identidade
privada `age` fora do projecto e com cópia segura: sem ela não é possível
recuperar os dados. O conteúdo de `backups/` é ignorado pelo Git. Esta cópia
local não protege contra perda ou falha do computador; para produção será
necessário definir armazenamento externo e uma política institucional.

## Resposta a incidentes

1. **Priorize a segurança clínica.** Se um fluxo essencial estiver
   indisponível, active o procedimento institucional de continuidade aprovado
   e informe o responsável clínico de serviço.
2. **Classifique e contenha.** Registe hora de início, sistemas e hospitais
   afectados, impacto e responsável pela coordenação. Em suspeita de acesso
   indevido, limite acessos e rode ou revogue credenciais através do gestor de
   segredos; preserve os registos necessários sem os copiar para canais
   públicos.
3. **Recupere de forma isolada.** Para corrupção ou perda, preserve a instância
   afectada para investigação, restaure um backup numa instância vazia e
   valide a integridade antes de aprovar qualquer regresso ao serviço.
4. **Comunique e documente.** Encaminhe incidentes de privacidade para o
   responsável institucional competente e cumpra os prazos legais aplicáveis.
   Não inclua nomes, identificadores de pacientes, tokens ou credenciais no
   ticket ou relatório.
5. **Feche com revisão.** Depois de estabilizar, documente a causa, a linha
   temporal, as decisões, a evidência preservada e as acções com responsáveis;
   actualize este runbook quando o processo mudar.

Não existe neste repositório uma escala de piquete, lista de contactos ou
objectivos de recuperação aprovados. A instituição deve designar os
responsáveis clínico, técnico e de privacidade, definir a cadeia de
escalonamento e validar RPO/RTO antes da entrada em produção.
