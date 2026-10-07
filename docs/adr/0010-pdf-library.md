# ADR-0010: Biblioteca para geração de PDF

- **Estado:** Aceite
- **Data:** 2026-10-07

## Contexto

O roadmap prevê a geração de documentos PDF, incluindo fichas de pacientes,
inventários e facturas. A biblioteca escolhida tem de ter uma licença
compatível com o projecto e suportar os caracteres e a apresentação em
português necessários.

## Decisão

Usar Apache PDFBox 3.0.8, sob licença Apache 2.0. Os geradores de relatórios e
facturas usam esta biblioteca; um teste automatizado verifica a geração,
leitura e extracção de texto português com acentos.

## Alternativas consideradas

- Uma biblioteca AGPL sem avaliação de impacto. Não recomendada devido às
  obrigações de licença que podem não corresponder ao modelo de distribuição.
- iText 9.8.0 sob AGPL ou licença comercial. Não adoptada para evitar as
  obrigações AGPL e a dependência de licença comercial.

## Consequências esperadas

- A dependência deve permanecer numa versão estável recente e ser verificada
  com testes de geração de PDF.
- Os documentos devem ser verificados quanto a acentos, fontes e
  identificadores visuais antes de produção.
