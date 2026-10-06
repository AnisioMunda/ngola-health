# ADR-0009: Facturação electrónica com a AGT

- **Estado:** Proposta
- **Data:** 2026-10-07

## Contexto

O roadmap prevê facturação electrónica integrada com a Administração Geral
Tributária (AGT). Os requisitos regulamentares e técnicos podem mudar e ainda
não foram confirmados para a implementação planeada.

## Proposta

A integração prevista deverá incluir:

- assinatura RSA dos documentos aplicáveis;
- cliente para a API da AGT;
- consulta do estado das submissões;
- utilização de ambiente de testes ou sandbox, quando disponibilizado;
- armazenamento das credenciais e da chave privada fora do repositório,
  carregadas de forma segura.

Antes da Fase 5, confirmar os requisitos vigentes, formatos, algoritmos,
certificados, endpoints, homologação e regras de operação directamente em
fontes oficiais da AGT. Registar os resultados e quaisquer alterações ao
âmbito numa revisão desta ADR.

## Alternativa considerada

Implementar com base em requisitos presumidos ou documentação desactualizada.
Rejeitada por poder produzir documentos ou integrações incompatíveis com as
obrigações vigentes.

## Consequências esperadas

- Nenhum endpoint, formato ou requisito fiscal deve ser tratado como
  confirmado até à validação oficial.
- A integração deve distinguir erros de validação, rejeições da AGT e falhas
  de rede.
- Credenciais de produção e chaves de assinatura não podem ser usadas em
  testes automatizados nem incluídas no repositório.

## Decisão pendente

Validar os requisitos actuais antes de iniciar a Fase 5 e actualizar esta ADR
com fontes, data da validação e decisão final.
