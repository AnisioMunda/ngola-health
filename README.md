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

## Stack prevista

As versões-alvo estão registadas na ADR-0002 do roadmap. A compatibilidade das
bibliotecas e ferramentas ainda será verificada antes da implementação da
fundação técnica.

| Área | Tecnologia prevista |
| --- | --- |
| Backend | Java 21 LTS, Spring Boot 4.1.x e Maven Wrapper |
| Frontend | Angular 22 |
| Base de dados | PostgreSQL 16 ou superior e Liquibase |
| Execução local | Docker Compose |

O protótipo actualmente existente no repositório declara versões diferentes em
algumas áreas, incluindo Spring Boot 3.2.5 e Angular 17. Esse código será
avaliado e alinhado com as decisões do roadmap; as versões previstas acima não
significam que essa migração já esteja concluída.

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
