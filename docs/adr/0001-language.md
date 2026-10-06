# ADR-0001: Idioma da documentação e do código

- **Estado:** Aceite
- **Data:** 2026-10-07

## Contexto

O projecto reúne documentação e interfaces destinadas a pessoas que usam
português, e código que beneficia de identificadores consistentes com o
ecossistema técnico. Sem uma regra comum, nomes de domínio, mensagens e
documentação podem divergir entre módulos.

## Decisão

- Escrever em português a documentação, os commits, os comentários, a
  interface e as mensagens de erro apresentadas às pessoas.
- Escrever em inglês os identificadores do domínio e do código: classes,
  tabelas, colunas, rotas, DTOs, papéis, SQL, nomes de ficheiros e pastas.
- Nas respostas da API, usar um `code` estável em inglês e uma `message` em
  português.

## Consequências

- A documentação e os textos visíveis às pessoas devem ser revistos em
  português.
- Os nomes técnicos permanecem consistentes em inglês entre backend, base de
  dados e frontend.
- Alterações de texto apresentado ao utilizador não devem traduzir nem
  renomear identificadores estáveis da API.

## Alternativas consideradas

- Usar português em todo o domínio e no código. Rejeitada para manter os
  identificadores técnicos consistentes e interoperáveis.
