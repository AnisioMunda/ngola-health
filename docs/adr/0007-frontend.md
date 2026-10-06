# ADR-0007: Arquitectura do frontend

- **Estado:** Proposta
- **Data:** 2026-10-07

## Contexto

O frontend precisa de suportar funcionalidades separadas por módulo, manter a
interface responsiva e reduzir divergências entre os contratos da API e os
tipos usados na aplicação.

## Proposta

- Usar Angular com componentes standalone e signals.
- Usar Angular Material para os componentes de interface.
- Organizar as funcionalidades em rotas lazy por módulo.
- Gerar os tipos consumidos pelo frontend a partir do contrato OpenAPI, em vez
  de manter cópias manuais dos DTOs.
- Manter componentes pequenos e reutilizáveis, com o texto visível às pessoas
  em português e identificadores de código em inglês.

## Alternativa considerada

Manter manualmente tipos de API no frontend. Rejeitada por permitir que os
tipos se desviem dos DTOs e do contrato real.

## Consequências esperadas

- O contrato OpenAPI deve ser mantido actualizado com a API.
- A geração de tipos deve ser repetível e integrada no fluxo de
  desenvolvimento.
- Alterações no contrato devem ser revistas juntamente com os consumidores
  afectados.
- A estrutura e as decisões de versão devem ser validadas ao inicializar o
  Angular previsto na ADR-0002.

## Decisão pendente

Confirmar a proposta e escolher a ferramenta concreta de geração de tipos na
Tarefa 7.6.
