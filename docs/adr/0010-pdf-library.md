# ADR-0010: Biblioteca para geração de PDF

- **Estado:** Proposta
- **Data:** 2026-10-07

## Contexto

O roadmap prevê a geração de documentos PDF, incluindo fichas de pacientes,
inventários e facturas. A biblioteca escolhida tem de ter uma licença
compatível com o projecto e suportar os caracteres e a apresentação em
português necessários.

## Proposta

Avaliar Apache PDFBox e OpenPDF e escolher uma biblioteca com licença
permissiva, compatível com a distribuição e utilização previstas para o
projecto. Não adoptar bibliotecas AGPL sem uma decisão explícita e uma revisão
das implicações de licença.

A escolha deve incluir um teste automatizado que gere e valide um PDF de
exemplo, incluindo texto com acentos e a apresentação correcta do logótipo
quando aplicável.

## Alternativas consideradas

- Uma biblioteca AGPL sem avaliação de impacto. Não recomendada devido às
  obrigações de licença que podem não corresponder ao modelo de distribuição.
- Uma biblioteca comercial com licença proprietária. Possível apenas após
  avaliação de custo, licença e compatibilidade.

## Consequências esperadas

- Confirmar licença, versão, manutenção e capacidades da biblioteca antes de
  a adicionar como dependência.
- A dependência deve ser adicionada na tarefa 3.4, com teste automatizado de
  geração de PDF.
- Os documentos devem ser verificados quanto a acentos, fontes e
  identificadores visuais.

## Decisão pendente

Escolher entre as opções avaliadas na Tarefa 3.4 e actualizar esta ADR com a
biblioteca, versão, licença e justificação.
