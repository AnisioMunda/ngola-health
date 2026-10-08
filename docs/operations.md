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
