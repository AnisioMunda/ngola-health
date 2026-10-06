# ADR-0008: Gestão de segredos

- **Estado:** Proposta
- **Data:** 2026-10-07

## Contexto

Credenciais, chaves de assinatura e outros segredos podem dar acesso a dados
clínicos, sistemas externos e ambientes de produção. Guardá-los no código,
histórico do Git ou exemplos partilhados cria risco de exposição difícil de
reverter.

## Proposta

- Nunca guardar segredos no repositório ou no histórico de commits.
- Versionar um `.env.example` sem valores secretos e manter configurações
  locais reais fora do Git.
- Executar gitleaks no CI para detectar padrões de segredos.
- Carregar chaves privadas e credenciais sensíveis a partir de ficheiros ou
  mecanismos externos de segredos, sem as embutir nas imagens.
- Em produção, falhar explicitamente ao arrancar se faltar um segredo
  obrigatório, sem recorrer a valores de demonstração.
- Não incluir credenciais reais, dados pessoais ou dados de saúde em exemplos,
  issues, logs ou documentação.

## Alternativa considerada

Guardar valores de desenvolvimento ou produção em ficheiros versionados.
Rejeitada por expor segredos no repositório e no seu histórico.

## Consequências esperadas

- Cada segredo obrigatório tem de ter origem e procedimento de rotação
  documentados fora dos valores secretos.
- Os ficheiros de ambiente locais devem ser ignorados pelo Git.
- A detecção automática de segredos complementa, mas não substitui, a revisão
  de alterações.
- Uma credencial exposta deve ser revogada e rodada; apagar o ficheiro do
  estado actual não remove o segredo do histórico.

## Decisão pendente

Confirmar os mecanismos concretos de carregamento para cada ambiente antes
das tarefas que os introduzem.
