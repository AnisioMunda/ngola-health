# Ngola Health

Sistema de gestão de saúde multi-hospital, pensado para apoiar as operações
clínicas e administrativas de instituições de saúde em Angola. O projecto está
a ser desenvolvido por etapas, com foco em segurança, qualidade e isolamento
dos dados de cada hospital.

## Objectivos

O Ngola Health pretende reunir, numa plataforma, capacidades como gestão de
utilizadores e hospitais, cadastro de pacientes, episódios clínicos, triagem,
farmácia, laboratório, agendamento, internamento, facturação e relatórios.
O portal do paciente e a telemedicina também fazem parte do âmbito planeado.

Estas capacidades são objectivos do projecto, não uma declaração de que todos
os módulos já estejam completos ou prontos para produção. O desenvolvimento
segue as tarefas e os critérios de conclusão descritos no
[roadmap](docs/ROADMAP.md).

## Stack adoptada

As versões adoptadas e as regras de actualização estão registadas na
[ADR-0002](docs/adr/0002-stack-and-versions.md). As dependências e ferramentas
do backend são mantidas nas versões estáveis mais recentes compatíveis e
validadas pelo build completo.

| Área | Tecnologia prevista |
| --- | --- |
| Backend | Java 25 LTS, Spring Boot 4.1.1 e Maven Wrapper |
| Frontend | Angular 22 |
| Base de dados | PostgreSQL 16 ou superior e Liquibase |
| Execução local | Docker Compose |

O backend foi alinhado com Java 25 e Spring Boot 4.1.1. O frontend e outros
componentes do protótipo ainda serão alinhados com as versões adoptadas nas
respectivas tarefas do roadmap.

## Estado do projecto

O projecto está na fase de fundação do repositório. As primeiras tarefas
organizam a configuração e a documentação antes de se avançar para a
consolidação do esqueleto técnico, dos testes e da integração contínua.

O código já presente em `backend/`, `frontend/` e `infra/` é um protótipo em
alinhamento com o roadmap. A sua presença no repositório, por si só, não
significa que os requisitos, testes ou critérios de conclusão de cada tarefa
estejam satisfeitos.

## Documentação

- [Roadmap, arquitectura e convenções de desenvolvimento](docs/ROADMAP.md)

O guia para instalar, executar, testar e depurar o sistema será acrescentado
na tarefa 1.18 do roadmap. Até lá, o projecto não declara um procedimento de
arranque local suportado.
