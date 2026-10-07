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

A consulta pública não forneceu os manuais técnicos do parceiro necessários
para confirmar o protocolo. Os campos `agt_*` já existentes na base de dados
são metadados internos provisórios; os seus nomes ou valores não constituem
uma confirmação do contrato da API da AGT.

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

Obter e validar a documentação técnica oficial do parceiro (incluindo
assinatura, formatos, endpoints, estados, QR Code, sandbox e homologação).
Depois actualizar esta ADR com a decisão técnica final antes de activar
qualquer transmissão real ou declarar conformidade.
