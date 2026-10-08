# ADR-0012: Modelo de telemedicina

- **Estado:** Aceite
- **Data:** 2026-10-08

## Contexto

A aplicação já permite agendar teleconsultas, mas o endereço anteriormente
gerado (`/video/{roomToken}`) não corresponde a uma sala implementada nem a
uma rota existente. É necessário escolher um modelo de comunicação antes de
implementar a API e os ecrãs da Fase 7.

## Decisão

- Usar ligações para um fornecedor externo de videoconferência; não construir
  WebRTC nem encaminhar áudio ou vídeo pela aplicação.
- Manter na aplicação os dados da teleconsulta e o estado do agendamento. A
  ligação deve ser emitida por um fornecedor aprovado e configurado para o
  ambiente; não se deve tratar um URL arbitrário fornecido pelo paciente como
  uma sala válida.
- Usar Microsoft Teams através do Microsoft Graph para criar as reuniões
  externas. Cada reunião é organizada pelo utilizador Microsoft Entra do médico
  seleccionado; o object ID é guardado no perfil do médico.
- Usar a permissão de aplicação `OnlineMeetings.ReadWrite.All` e uma
  application access policy limitada às contas organizadoras autorizadas.
  Credenciais da aplicação são injectadas por variáveis de ambiente, nunca
  guardadas na base de dados ou no repositório.
- Criar reuniões independentes do calendário com o assunto genérico
  "Consulta médica", sem nome do paciente nem dados clínicos. As reuniões
  exigem que o organizador esteja presente para permitir a entrada.
- Não gravar nem armazenar áudio ou vídeo. A ligação da sala é informação
  sensível: só pode ser devolvida a participantes autorizados, não deve ser
  incluída em logs e deve abrir-se no browser com protecção contra acesso à
  janela de origem.

## Alternativa considerada

Implementar WebRTC dentro da aplicação. Rejeitada nesta fase por exigir
infraestrutura e trabalho adicional de transmissão, compatibilidade,
segurança e privacidade, sem necessidade funcional que o justifique agora.

## Consequências e revisões necessárias

- A ligação interna inexistente foi retirada da resposta da API. Até a
  integração Microsoft Teams estar configurada, a interface não permite criar
  sessões.
- A integração valida que o endereço devolvido usa HTTPS e pertence ao domínio
  `teams.microsoft.com`. Só médicos activos com object ID Microsoft Entra
  configurado podem organizar sessões.
- A duração da reunião é indicada na criação, entre 1 e 1440 minutos.
- Antes de activar a integração, o administrador do tenant Microsoft tem de
  conceder a permissão de aplicação, criar a application access policy e
  atribuí-la aos médicos organizadores.
- Referências de configuração: [criar uma reunião online no Microsoft
  Graph](https://learn.microsoft.com/graph/api/application-post-onlinemeetings?view=graph-rest-1.0)
  e [configurar uma application access
  policy](https://learn.microsoft.com/graph/cloud-communication-online-meeting-application-access-policy).
- Os requisitos de privacidade, retenção e localização dos dados do fornecedor
  devem ser revistos antes de disponibilizar a funcionalidade em produção.
- Uma futura decisão de construir WebRTC exige uma ADR própria e uma revisão
  dos requisitos de infraestrutura e privacidade.
