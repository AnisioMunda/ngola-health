# Política de segurança

O Ngola Health trata informação de saúde, pelo que a segurança deve ser
considerada em todas as fases de desenvolvimento. Esta política explica como
comunicar vulnerabilidades e como lidar com segredos expostos.

## Versões abrangidas

O projecto ainda não tem uma versão estável publicada. Durante esta fase,
comunicar vulnerabilidades que afectem a branch `main` e qualquer versão
publicada que ainda esteja em utilização. Quando existirem versões suportadas,
esta secção será actualizada com a respectiva política.

## Reportar uma vulnerabilidade

**Não abrir uma issue pública nem um pull request com detalhes de uma
vulnerabilidade não corrigida.**

Usar a funcionalidade de reporte privado de vulnerabilidades do GitHub na
página **Security** do repositório, se estiver disponível. Se não estiver
disponível, contactar em privado um mantenedor do projecto através do GitHub
antes de divulgar detalhes. Não publicar a vulnerabilidade enquanto não
existir um canal privado de coordenação.

Incluir no reporte, quando possível:

- uma descrição do problema e do impacto potencial;
- os componentes, versões ou commits afectados;
- passos de reprodução seguros e mínimos;
- pré-requisitos e configuração necessários;
- uma sugestão de mitigação, se conhecida.

Não incluir palavras-passe, tokens, chaves privadas, dados pessoais, dados de
saúde reais ou cópias de bases de dados. Usar valores sintéticos e remover
segredos de capturas de ecrã, registos e provas de conceito.

Os mantenedores devem confirmar a recepção e coordenar a análise, a correcção
e a divulgação com quem reportou. O prazo de resposta depende da gravidade e
da capacidade de manutenção; não é garantido um SLA. A divulgação pública
deve ocorrer depois de existir uma mitigação ou correcção adequada e de se
coordenar o anúncio com quem reportou.

## Política de segredos

**Nunca guardar segredos no Git.** Isto inclui o estado actual dos ficheiros e
todo o histórico de commits, branches, tags e pull requests. Exemplos de
segredos:

- palavras-passe e credenciais de bases de dados ou serviços;
- tokens de acesso, chaves de API e segredos JWT;
- chaves privadas, certificados e credenciais da AGT;
- credenciais de serviços de email, armazenamento ou infraestrutura.

Regras para desenvolvimento e contribuição:

- Manter `.env` e outros ficheiros locais fora do repositório; estes devem
  permanecer ignorados pelo Git.
- O `.env.example` pode ser versionado apenas como modelo, sem valores
  secretos, credenciais funcionais ou dados reais.
- Carregar segredos de ambientes protegidos ou de ficheiros montados fora da
  imagem e do repositório, conforme os mecanismos definidos para cada
  ambiente.
- Nunca colocar segredos ou dados reais de pacientes em código, testes,
  documentação, issues, pull requests, logs ou exemplos.
- A verificação automática de segredos no CI complementa a revisão; não torna
  aceitável adicionar valores sensíveis.

### Se um segredo for exposto

Tratar qualquer segredo publicado ou enviado a um canal não autorizado como
comprometido:

1. Revogar ou desactivar imediatamente a credencial exposta.
2. Gerar e distribuir um novo segredo através do canal seguro apropriado.
3. Verificar actividade potencialmente não autorizada e preservar os registos
   necessários à investigação, sem os divulgar publicamente.
4. Comunicar o incidente em privado aos mantenedores e às partes responsáveis
   pelo sistema afectado.
5. Remover o valor do estado activo do código e avaliar, com os mantenedores,
   a limpeza do histórico e das cópias aplicáveis.

Apagar o valor do último commit não o remove do histórico nem torna segura a
credencial antiga. A revogação e a rotação são prioritárias.

## Âmbito da política

Esta política complementa os requisitos de segurança e a gestão de segredos
descritos no [roadmap](docs/ROADMAP.md), na
[ADR-0008](docs/adr/0008-secrets-management.md) e no
[guia de contribuição](CONTRIBUTING.md).
