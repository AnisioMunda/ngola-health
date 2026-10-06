# ADR-0006: Infraestrutura por caso de uso

- **Estado:** Proposta
- **Data:** 2026-10-07

## Contexto

Serviços de infraestrutura aumentam os requisitos de operação, configuração,
segurança e manutenção. A sua inclusão antes de existir uma necessidade
concreta cria complexidade e segredos adicionais sem benefício comprovado.

## Proposta

- O Compose inicial deve incluir apenas PostgreSQL, backend e frontend.
- Adicionar Redis, RabbitMQ, MinIO, SMTP ou WebSocket apenas quando existir um
  caso de uso concreto e uma decisão de arquitectura aprovada.
- Registar a necessidade, alternativas, impacto operacional e configuração
  segura na ADR correspondente antes de adicionar cada serviço.

## Alternativa considerada

Incluir desde o início todos os serviços que possam vir a ser úteis. Rejeitada
por aumentar a complexidade sem requisitos actuais.

## Consequências esperadas

- Cada serviço externo deve ter uma funcionalidade e uma responsabilidade
  identificáveis.
- Serviços não usados devem ser removidos da configuração e dos manifestos.
- A aplicação deve falhar de forma explícita quando um serviço obrigatório
  estiver indisponível, sem recorrer a resultados de sucesso fictícios.

## Decisão pendente

Confirmar esta proposta durante a revisão das ADRs e antes de consolidar o
Compose inicial.
