# ADR-0002: Stack e versões

- **Estado:** Aceite
- **Data:** 2026-10-07

## Contexto

O projecto precisa de uma stack suportada e coerente para backend, frontend,
base de dados e migrações. A escolha deve permitir manter o sistema,
automatizar testes e validar as bibliotecas necessárias antes de consolidar
o esqueleto técnico.

## Decisão aceite

- Java 25 LTS para o backend.
- Spring Boot 4.1.1 para a aplicação backend, a versão estável mais recente
  confirmada na implementação da Tarefa 1.1.
- Maven Wrapper para builds reproduzíveis.
- Angular 22 para o frontend.
- PostgreSQL 16 ou superior como base de dados relacional.
- Liquibase para migrações, seguindo a convenção da ADR-0005.
- Apache PDFBox 3.0.8 para geração de PDF, sob licença Apache 2.0.

As versões efectivamente usadas no backend são verificadas e fixadas no
`pom.xml`. Dependências e plugins devem acompanhar as versões estáveis mais
recentes compatíveis, com o build e os testes completos a validar cada
actualização. A matriz de versões deve ser revista antes de novas tarefas que
acrescentem dependências.

## Alternativas consideradas

- Usar uma versão anterior de Spring Boot ou Java. Só deve ser considerada se
  uma incompatibilidade comprovada impedir o uso das versões estáveis actuais,
  ficando a excepção documentada nesta ADR.

## Consequências esperadas

- Java 25 é a linha LTS adoptada; imagens Docker, CI e builds locais usam a
  mesma versão principal.
- Actualizações estáveis de dependências e plugins são validadas por
  `clean verify` antes de serem aceites.
- Dependências geridas pelo Spring Boot mantêm as versões coerentes do BOM;
  versões explícitas só são usadas quando necessárias.

## Decisão

Adoptar Java 25 LTS, a versão estável mais recente de Spring Boot compatível
com o backend, e manter as dependências e plugins nas versões estáveis mais
recentes que passam a validação completa do projecto.
