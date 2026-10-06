# ADR-0002: Stack e versões

- **Estado:** Proposta
- **Data:** 2026-10-07

## Contexto

O projecto precisa de uma stack suportada e coerente para backend, frontend,
base de dados e migrações. A escolha deve permitir manter o sistema,
automatizar testes e validar as bibliotecas necessárias antes de consolidar
o esqueleto técnico.

## Proposta

- Java 21 LTS para o backend.
- Spring Boot 4.1.x para a aplicação backend.
- Maven Wrapper para builds reproduzíveis.
- Angular 22 para o frontend.
- PostgreSQL 16 ou superior como base de dados relacional.
- Liquibase para migrações, seguindo a convenção da ADR-0005.

Na Tarefa 1.1, confirmar a compatibilidade destas versões com as bibliotecas
necessárias, incluindo springdoc, jjwt, MapStruct, Lombok e a biblioteca de
PDF. Se houver um bloqueio, avaliar Spring Boot 3.5.x e registar um plano de
migração e a versão escolhida antes de iniciar a implementação dependente.

## Alternativas consideradas

- Manter Spring Boot 3.5.x e Angular 20. Não é a proposta inicial; pode ser
  adoptada se a verificação de compatibilidade revelar um bloqueio nas versões
  propostas.

## Consequências esperadas

- A fundação técnica não deve considerar estas versões confirmadas até a
  compatibilidade ser verificada.
- As versões efectivamente escolhidas devem ser fixadas nos manifestos e
  documentadas quando a Tarefa 1.1 for concluída.
- Actualizações de versão posteriores devem ser avaliadas quanto a
  compatibilidade e impacto nos módulos.

## Decisão pendente

Validar as versões e bibliotecas na Tarefa 1.1. Se for necessária uma
alternativa, actualizar esta ADR e documentar o plano de migração.
