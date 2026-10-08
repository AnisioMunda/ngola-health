# Ngola Health

Sistema de gestão de saúde multi-hospital, pensado para apoiar as operações
clínicas e administrativas de instituições de saúde em Angola. O projecto está
a ser desenvolvido por etapas, com foco em segurança, qualidade e isolamento
dos dados de cada hospital.

## Objectivos

O Ngola Health pretende reunir, numa plataforma, capacidades como gestão de
utilizadores e hospitais, cadastro de pacientes, episódios clínicos, triagem,
farmácia, laboratório, agendamento, internamento, facturação e relatórios.
O portal do paciente e a telemedicina com Microsoft Teams também estão
implementados; a integração externa requer configuração e revisão operacional
antes de ser disponibilizada em produção.

Estas capacidades são objectivos do projecto, não uma declaração de que todos
os módulos já estejam completos ou prontos para produção. O desenvolvimento
segue as tarefas e os critérios de conclusão descritos no
[roadmap](docs/ROADMAP.md).

## Stack adoptada

As versões adoptadas e as regras de actualização estão registadas na
[ADR-0002](docs/adr/0002-stack-and-versions.md). As dependências e ferramentas
do backend são mantidas nas versões estáveis mais recentes compatíveis e
validadas pelo build completo.

| Área           | Tecnologia prevista                            |
| -------------- | ---------------------------------------------- |
| Backend        | Java 25 LTS, Spring Boot 4.1.1 e Maven Wrapper |
| Frontend       | Angular 22                                     |
| Base de dados  | PostgreSQL 16 ou superior e Liquibase          |
| Execução local | Docker Compose                                 |

O backend e o frontend estão alinhados com Java 25, Spring Boot 4.1.1 e
Angular 22. A versão PostgreSQL usada no ambiente local está definida em
`docker-compose.yml`.

## Estado do projecto

O projecto concluiu a fundação técnica da Fase 1: backend Java 25, frontend
Angular 22, execução local com Docker Compose, testes e integração contínua.
As capacidades clínicas e administrativas continuam em desenvolvimento
conforme o [roadmap](docs/ROADMAP.md); a presença de ecrãs ou código de
protótipo não significa que os módulos estejam prontos para produção.

Os componentes funcionais existentes em `backend/`, `frontend/` e `infra/`
estão a ser alinhados com os requisitos e as decisões arquitecturais do
projecto.

## Documentação

- [Guia de desenvolvimento: instalar, executar, testar e depurar](docs/development.md)
- [Guia de testes manuais ponta a ponta, por ordem de dependência](docs/manual-testing-guide.md)
- [Segurança, autenticação, perfis e isolamento multi-hospital](docs/security.md)
- [Fluxo clínico e regras de negócio implementadas](docs/clinical-workflow.md)
- [Agendamento, notificações e internamento](docs/scheduling-notifications-inpatient.md)
- [Gestão de RH, equipamentos, dashboards e relatórios](docs/operations.md)
- [Portal do paciente e telemedicina com Microsoft Teams](docs/operations.md#portal-do-paciente)
- [Roadmap, arquitectura e convenções de desenvolvimento](docs/ROADMAP.md)
- [Registo de alterações](CHANGELOG.md) e [notas de preparação da versão 1.0.0](docs/release-notes-1.0.0.md)
