package ao.hospitalao.modules.telemedicine.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class MicrosoftTeamsMeetingProvider implements TeamsMeetingProvider {

  private static final String GRAPH_URL = "https://graph.microsoft.com/v1.0";
  private static final String TOKEN_URL = "https://login.microsoftonline.com/%s/oauth2/v2.0/token";
  private static final String GRAPH_SCOPE = "https://graph.microsoft.com/.default";

  private final TeamsProperties properties;
  private final RestClient.Builder restClientBuilder;

  @Override
  public boolean isConfigured() {
    return properties.isEnabled();
  }

  @Override
  public TeamsMeeting createMeeting(
      OffsetDateTime scheduledAt, int durationMinutes, String organizerId) {
    if (!properties.isEnabled()) {
      throw new IllegalStateException("A integração Microsoft Teams não está configurada.");
    }
    if (organizerId == null || organizerId.isBlank()) {
      throw new IllegalStateException("O Microsoft Entra ID do médico não está configurado.");
    }

    RestClient client = restClientBuilder.build();
    String accessToken = requestAccessToken(client);
    OnlineMeetingResponse response =
        requestMeeting(client, accessToken, scheduledAt, durationMinutes, organizerId);

    if (response == null || isBlank(response.id()) || isBlank(response.joinWebUrl())) {
      throw new TeamsMeetingProviderException(
          "O Microsoft Graph devolveu uma reunião sem identificador ou ligação.");
    }
    validateJoinUrl(response.joinWebUrl());
    return new TeamsMeeting(response.id(), response.joinWebUrl());
  }

  @Override
  public void deleteMeeting(String meetingId, String organizerId) {
    if (!properties.hasCredentials()) {
      throw new IllegalStateException(
          "As credenciais Microsoft Teams são necessárias para revogar a ligação da reunião.");
    }
    if (isBlank(meetingId) || isBlank(organizerId)) {
      throw new IllegalArgumentException(
          "A reunião e o organizador Microsoft Teams são obrigatórios.");
    }

    RestClient client = restClientBuilder.build();
    String accessToken = requestAccessToken(client);
    try {
      client
          .delete()
          .uri(
              GRAPH_URL + "/users/{organizerId}/onlineMeetings/{meetingId}", organizerId, meetingId)
          .headers(headers -> headers.setBearerAuth(accessToken))
          .retrieve()
          .onStatus(status -> status.value() == 404, (request, response) -> {})
          .toBodilessEntity();
    } catch (RestClientResponseException exception) {
      throw new TeamsMeetingProviderException(
          "O Microsoft Graph não conseguiu revogar a reunião (HTTP %d)."
              .formatted(exception.getStatusCode().value()));
    } catch (ResourceAccessException exception) {
      throw new TeamsMeetingProviderException(
          "Não foi possível contactar o Microsoft Graph para revogar a reunião.", exception);
    }
  }

  private String requestAccessToken(RestClient client) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "client_credentials");
    form.add("client_id", properties.getClientId());
    form.add("client_secret", properties.getClientSecret());
    form.add("scope", GRAPH_SCOPE);

    try {
      AccessTokenResponse response =
          client
              .post()
              .uri(TOKEN_URL.formatted(properties.getTenantId()))
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(form)
              .retrieve()
              .body(AccessTokenResponse.class);
      if (response == null || isBlank(response.accessToken())) {
        throw new IllegalStateException("A plataforma Microsoft não devolveu um token de acesso.");
      }
      return response.accessToken();
    } catch (RestClientResponseException exception) {
      throw new TeamsMeetingProviderException(
          "A plataforma Microsoft rejeitou a autenticação (HTTP %d)."
              .formatted(exception.getStatusCode().value()));
    } catch (ResourceAccessException exception) {
      throw new TeamsMeetingProviderException(
          "Não foi possível contactar a plataforma Microsoft.", exception);
    }
  }

  private OnlineMeetingResponse requestMeeting(
      RestClient client,
      String accessToken,
      OffsetDateTime scheduledAt,
      int durationMinutes,
      String organizerId) {
    OnlineMeetingRequest request =
        new OnlineMeetingRequest(
            scheduledAt,
            scheduledAt.plusMinutes(durationMinutes),
            "Consulta médica",
            Map.of("scope", "organizer"));

    try {
      return client
          .post()
          .uri(GRAPH_URL + "/users/{organizerId}/onlineMeetings", organizerId)
          .headers(headers -> headers.setBearerAuth(accessToken))
          .contentType(MediaType.APPLICATION_JSON)
          .body(request)
          .retrieve()
          .body(OnlineMeetingResponse.class);
    } catch (RestClientResponseException exception) {
      throw new TeamsMeetingProviderException(
          "O Microsoft Graph não conseguiu criar a reunião (HTTP %d)."
              .formatted(exception.getStatusCode().value()));
    } catch (ResourceAccessException exception) {
      throw new TeamsMeetingProviderException(
          "Não foi possível contactar o Microsoft Graph para criar a reunião.", exception);
    }
  }

  private void validateJoinUrl(String value) {
    URI uri;
    try {
      uri = URI.create(value);
    } catch (IllegalArgumentException exception) {
      throw new TeamsMeetingProviderException(
          "O Microsoft Graph devolveu uma ligação de reunião inválida.");
    }

    String host = uri.getHost();
    if (!"https".equalsIgnoreCase(uri.getScheme())
        || host == null
        || !(host.equals("teams.microsoft.com") || host.endsWith(".teams.microsoft.com"))) {
      throw new TeamsMeetingProviderException(
          "O Microsoft Graph devolveu uma ligação que não pertence ao Microsoft Teams.");
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private record AccessTokenResponse(@JsonProperty("access_token") String accessToken) {}

  private record OnlineMeetingRequest(
      OffsetDateTime startDateTime,
      OffsetDateTime endDateTime,
      String subject,
      Map<String, String> lobbyBypassSettings) {}

  private record OnlineMeetingResponse(String id, String joinWebUrl) {}
}
