package ao.hospitalao.modules.financial.agt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

class AgtApiClientTest {

  @Test
  void doesNotSendInvoiceWhenIntegrationIsDisabled() throws Exception {
    var properties = new AgtProperties();
    properties.setEnabled(false);
    var restTemplate = mock(RestTemplate.class);
    var client =
        new AgtApiClient(
            properties, mock(AgtSigningService.class), new ObjectMapper(), restTemplate);

    var result = client.register(null);

    assertThat(result.status()).isEqualTo("ERROR");
    assertThat(result.errorMessage()).contains("desactivada");
    verifyNoInteractions(restTemplate);
  }

  @Test
  void doesNotPollOrRequestSeriesWhenIntegrationIsDisabled() throws Exception {
    var properties = new AgtProperties();
    properties.setEnabled(false);
    var restTemplate = mock(RestTemplate.class);
    var client =
        new AgtApiClient(
            properties, mock(AgtSigningService.class), new ObjectMapper(), restTemplate);

    assertThat(client.checkStatus("request-1").status()).isEqualTo("UNKNOWN");
    assertThatThrownBy(() -> client.requestSeries("FT", 2026))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("desactivada");
    verifyNoInteractions(restTemplate);
  }
}
