package ao.hospitalao.modules.telemedicine.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MicrosoftTeamsMeetingProviderTest {

  @Test
  void createsMeetingForTheSelectedDoctorWithoutIncludingPatientData() {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    var properties = configuredProperties();
    var provider = new MicrosoftTeamsMeetingProvider(properties, builder);
    String organizerId = "c7ad8e3d-a0a6-4d41-b44f-06c31bc662c4";
    OffsetDateTime scheduledAt = OffsetDateTime.parse("2027-01-10T14:30:00Z");

    server
        .expect(requestTo("https://login.microsoftonline.com/tenant-1/oauth2/v2.0/token"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
        .andRespond(
            withSuccess("{\"access_token\":\"graph-access-token\"}", MediaType.APPLICATION_JSON));
    server
        .expect(
            requestTo("https://graph.microsoft.com/v1.0/users/" + organizerId + "/onlineMeetings"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer graph-access-token"))
        .andExpect(jsonPath("$.startDateTime").value("2027-01-10T14:30:00Z"))
        .andExpect(jsonPath("$.endDateTime").value("2027-01-10T15:15:00Z"))
        .andExpect(jsonPath("$.subject").value("Consulta médica"))
        .andExpect(jsonPath("$.lobbyBypassSettings.scope").value("organizer"))
        .andExpect(content().string(containsString("organizer")))
        .andRespond(
            withSuccess(
                "{\"id\":\"meeting-1\",\"joinWebUrl\":\"https://teams.microsoft.com/l/meetup-join/meeting-1\"}",
                MediaType.APPLICATION_JSON));

    var meeting = provider.createMeeting(scheduledAt, 45, organizerId);

    assertThat(meeting.id()).isEqualTo("meeting-1");
    assertThat(meeting.joinUrl()).isEqualTo("https://teams.microsoft.com/l/meetup-join/meeting-1");
    server.verify();
  }

  @Test
  void treatsAnAlreadyDeletedTeamsMeetingAsSuccessfullyRevoked() {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    var provider = new MicrosoftTeamsMeetingProvider(configuredProperties(), builder);

    server
        .expect(requestTo("https://login.microsoftonline.com/tenant-1/oauth2/v2.0/token"))
        .andRespond(
            withSuccess("{\"access_token\":\"graph-access-token\"}", MediaType.APPLICATION_JSON));
    server
        .expect(
            requestTo(
                "https://graph.microsoft.com/v1.0/users/organizer-id/onlineMeetings/meeting-id"))
        .andExpect(method(HttpMethod.DELETE))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    provider.deleteMeeting("meeting-id", "organizer-id");

    server.verify();
  }

  @Test
  void rejectsMeetingLinksOutsideTheMicrosoftTeamsDomain() {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    var provider = new MicrosoftTeamsMeetingProvider(configuredProperties(), builder);
    OffsetDateTime scheduledAt = OffsetDateTime.parse("2027-01-10T14:30:00Z");

    server
        .expect(requestTo("https://login.microsoftonline.com/tenant-1/oauth2/v2.0/token"))
        .andRespond(
            withSuccess("{\"access_token\":\"graph-access-token\"}", MediaType.APPLICATION_JSON));
    server
        .expect(requestTo("https://graph.microsoft.com/v1.0/users/organizer-id/onlineMeetings"))
        .andRespond(
            withSuccess(
                "{\"id\":\"meeting-1\",\"joinWebUrl\":\"https://attacker.example/join\"}",
                MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> provider.createMeeting(scheduledAt, 30, "organizer-id"))
        .isInstanceOf(TeamsMeetingProviderException.class)
        .hasMessageContaining("não pertence ao Microsoft Teams");
    server.verify();
  }

  @Test
  void deletesTeamsMeetingsUsingTheSavedOrganizerId() {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    var provider = new MicrosoftTeamsMeetingProvider(configuredProperties(), builder);

    server
        .expect(requestTo("https://login.microsoftonline.com/tenant-1/oauth2/v2.0/token"))
        .andRespond(
            withSuccess("{\"access_token\":\"graph-access-token\"}", MediaType.APPLICATION_JSON));
    server
        .expect(
            requestTo(
                "https://graph.microsoft.com/v1.0/users/organizer-id/onlineMeetings/meeting-id"))
        .andExpect(method(HttpMethod.DELETE))
        .andExpect(header("Authorization", "Bearer graph-access-token"))
        .andRespond(withNoContent());

    provider.deleteMeeting("meeting-id", "organizer-id");

    server.verify();
  }

  @Test
  void doesNotCallMicrosoftWhenTheIntegrationIsDisabled() {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    var properties = configuredProperties();
    properties.setEnabled(false);
    var provider = new MicrosoftTeamsMeetingProvider(properties, builder);

    assertThatThrownBy(
            () ->
                provider.createMeeting(
                    OffsetDateTime.parse("2027-01-10T14:30:00Z"), 30, "organizer-id"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("não está configurada");
    server.verify();
  }

  private TeamsProperties configuredProperties() {
    var properties = new TeamsProperties();
    properties.setEnabled(true);
    properties.setTenantId("tenant-1");
    properties.setClientId("client-1");
    properties.setClientSecret("test-secret");
    return properties;
  }
}
