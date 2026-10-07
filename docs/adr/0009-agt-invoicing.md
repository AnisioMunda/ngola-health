# ADR-0009: Facturação electrónica com a AGT

- **Estado:** Proposta — validação técnica pendente
- **Data:** 2026-10-07

## Contexto

O roadmap prevê facturação electrónica integrada com a Administração Geral
Tributária (AGT). A consulta das publicações públicas da AGT em 2026-10-07
confirma a existência do novo Regime Jurídico das Facturas, a implementação
faseada da facturação electrónica e a exigência de utilização de software
certificado nos casos abrangidos. Estas publicações não especificam a API, o
formato dos pedidos, os algoritmos de assinatura, os estados de resposta, o
conteúdo do QR Code nem a disponibilidade de sandbox. Esses detalhes têm de
ser obtidos na documentação técnica oficial destinada aos parceiros e
integradores antes de implementar ou homologar a comunicação.

Desde essa consulta foi possível aceder a páginas técnicas públicas de
facturação electrónica no domínio da AGT. Elas descrevem endpoints de
homologação, Basic Authentication, submissões JSON assíncronas, JWS RS256 e
consulta de estado. A sua existência não substitui a validação do contrato
actual do parceiro: há divergências internas nas próprias páginas, a versão
aplicável não está confirmada e os exemplos não permitem provar conformidade
do modelo fiscal deste projecto.

## Proposta

A solução deverá cumprir os requisitos técnicos e legais oficiais para o
software aplicável, incluindo a assinatura digital qualificada quando exigida.
O algoritmo, os dados assinados, o formato do documento, o canal de
comunicação, a consulta de estado, o QR Code e o fluxo de homologação ficam
por decidir até à obtenção da documentação oficial. Credenciais e chaves
continuam a ser carregadas de forma segura a partir do ambiente de execução,
nunca do repositório.

### Validação pública efectuada em 2026-10-07

Fontes oficiais consultadas:

- [AGT — Novo Regime Jurídico das Facturas](https://agt.minfin.gov.ao/PortalAGT/?#!/sala-de-imprensa/noticias/14532/novo-regime-juridico-das-facturas-marca-inicio-da-facturacao-electronica-em-angola):
  enquadramento público do novo regime.
- [AGT — Lista de softwares certificados para emissão de factura electrónica](https://agt.minfin.gov.ao/PortalAGT/#!/sala-de-imprensa/noticias/14664/lista-de-softwares-certificados-para-emissao-de-factura-electronica):
  existência de certificação e lista de soluções.
- [AGT — Facturação electrónica: bem-vindos ao futuro](https://agt.minfin.gov.ao/PortalAGT/?#!/sala-de-imprensa/noticias/14717/facturacao-electronica-bem-vindos-ao-futuro):
  informação pública sobre a implementação.
- [AGT — Legislação fiscal](https://agt.minfin.gov.ao/PortalAGT/?index=2&#!/legislacao/fiscal):
  portal oficial para consulta da legislação, incluindo o Decreto Presidencial
  n.º 71/25.
- [AGT — Introdução técnica à facturação electrónica](https://quiosqueagt.minfin.gov.ao/doc-agt/faturacao-electronica/1/index.html):
  descreve o fluxo assíncrono e consulta posterior do estado.
- [AGT — Autenticação e autorização](https://quiosqueagt.minfin.gov.ao/doc-agt/faturacao-electronica/1/api.html):
  documenta Basic Authentication e o pedido de credenciais ao produtor.
- [AGT — Estrutura das assinaturas digitais](https://quiosqueagt.minfin.gov.ao/doc-agt/faturacao-electronica/1/estrutura.html):
  descreve JWS RS256 e payloads de assinatura.
- [AGT — Registar factura electrónica](https://quiosqueagt.minfin.gov.ao/doc-agt/faturacao-electronica/1/servicos/registar.html):
  publica os endpoints e o esquema de submissão.
- [AGT — Consultar estado da factura](https://quiosqueagt.minfin.gov.ao/doc-agt/faturacao-electronica/1/servicos/consultar.html):
  publica o endpoint de consulta de estado.

As páginas consultadas apresentam os seguintes pontos por validar com o
parceiro:

- A secção de estrutura descreve JWS Compact Serialization e cabeçalho
  `RS256`/`JWT`, mas a tabela do serviço de registo indica tamanho fixo de
  256 caracteres para `jwsDocumentSignature`.
- Os exemplos do registo exigem dados SAF-T como `eacCode`, código e unidade
  do produto/serviço, que ainda não existem no modelo de facturação.
- A página de registo não mostra `jwsSignature` no exemplo do pedido, enquanto
  a documentação geral descreve a assinatura de requisições com payload
  variável.
- Não estão disponíveis credenciais autorizadas de homologação nem confirmação
  de qual versão do manual se aplica ao produtor.

Os campos `agt_*` já existentes na base de dados são metadados internos
provisórios; os seus nomes ou valores não constituem uma confirmação do
contrato da API da AGT. A integração está desactivada por omissão
(`AGT_ENABLED=false`) para impedir o envio de dados de pacientes antes da
validação.

## Alternativa considerada

Implementar com base em requisitos presumidos ou documentação desactualizada.
Rejeitada por poder produzir documentos ou integrações incompatíveis com as
obrigações vigentes.

## Consequências esperadas

- As páginas públicas são referências técnicas, não prova de certificação nem
  homologação deste produto.
- Nenhum payload fiscal deve ser enviado até se confirmar a versão do esquema,
  o mapeamento SAF-T, a representação das assinaturas e as respostas do serviço.
- A integração deve distinguir erros de validação, rejeições da AGT e falhas
  de rede.
- Credenciais de produção e chaves de assinatura não podem ser usadas em
  testes automatizados nem incluídas no repositório.

## Decisão pendente

Obter do parceiro a versão aplicável do esquema e os esclarecimentos acima,
com credenciais sandbox fornecidas fora do repositório. Depois actualizar
esta ADR e testar a integração em homologação antes de activar transmissões
reais ou declarar conformidade.
