package ao.hospitalao.modules.financial.agt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import ao.hospitalao.modules.financial.entity.Invoice;
import ao.hospitalao.modules.financial.entity.Invoice.DocumentType;
import ao.hospitalao.modules.financial.entity.InvoiceItem;
import ao.hospitalao.modules.financial.entity.ServicePrice;
import ao.hospitalao.modules.patients.entity.Patient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AgtApiClientTest {

  @Test
  void doesNotSendInvoiceWhenIntegrationIsDisabled() throws Exception {
    var fixture = fixture(false);

    var result = fixture.client().register(null);

    assertThat(result.status()).isEqualTo("ERROR");
    assertThat(result.errorMessage()).contains("desactivada");
    fixture.server().verify();
  }

  @Test
  void registersInvoiceUsingDocumentedEndpointAndBasicAuthentication() throws Exception {
    var fixture = fixture(true);
    fixture
        .server()
        .expect(requestTo("https://sifphml.minfin.gov.ao/sigt/fe/v1/registarFactura"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", basicAuth("sandbox-user", "sandbox-password")))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string(containsString("schemaVersion")))
        .andExpect(jsonPath("$.schemaVersion").value("2.0"))
        .andExpect(jsonPath("$.numberOfEntries").value(1))
        .andExpect(jsonPath("$.documents[0].documentNo").value("FT 2026/0000001"))
        .andExpect(jsonPath("$.documents[0].documentStatus").value("N"))
        .andExpect(jsonPath("$.documents[0].lines[0].productCode").value("CONS-001"))
        .andExpect(jsonPath("$.documents[0].lines[0].taxes[0].taxCode").value("NOR"))
        .andRespond(
            withSuccess(
                """
                  {"requestID":"202600000010689","errorList":[]}
                  """,
                MediaType.APPLICATION_JSON));

    var result = fixture.client().register(invoice());

    assertThat(result.requestId()).isEqualTo("202600000010689");
    assertThat(result.status()).isEqualTo("PENDING");
    fixture.server().verify();
  }

  @Test
  void retriesTransientServerErrors() throws Exception {
    var fixture = fixture(true);
    fixture
        .server()
        .expect(requestTo("https://sifphml.minfin.gov.ao/sigt/fe/v1/registarFactura"))
        .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
    fixture
        .server()
        .expect(requestTo("https://sifphml.minfin.gov.ao/sigt/fe/v1/registarFactura"))
        .andRespond(
            withSuccess(
                """
                  {"requestID":"202600000010689","errorList":[]}
                  """,
                MediaType.APPLICATION_JSON));

    var result = fixture.client().register(invoice());

    assertThat(result.requestId()).isEqualTo("202600000010689");
    assertThat(result.status()).isEqualTo("PENDING");
    fixture.server().verify();
  }

  @Test
  void returnsClearNetworkFailureAfterFinalAttempt() throws Exception {
    var fixture = fixture(true);
    fixture
        .server()
        .expect(requestTo("https://sifphml.minfin.gov.ao/sigt/fe/v1/registarFactura"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withException(new IOException("temporary network failure")));
    fixture
        .server()
        .expect(requestTo("https://sifphml.minfin.gov.ao/sigt/fe/v1/registarFactura"))
        .andRespond(withException(new IOException("temporary network failure")));
    fixture
        .server()
        .expect(requestTo("https://sifphml.minfin.gov.ao/sigt/fe/v1/registarFactura"))
        .andRespond(withException(new IOException("temporary network failure")));

    var result = fixture.client().register(invoice());

    assertThat(result.status()).isEqualTo("ERROR");
    assertThat(result.errorMessage()).contains("3 tentativas");
    fixture.server().verify();
  }

  @Test
  void doesNotRetryNonTransientClientErrors() throws Exception {
    var fixture = fixture(true);
    fixture
        .server()
        .expect(requestTo("https://sifphml.minfin.gov.ao/sigt/fe/v1/registarFactura"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.BAD_REQUEST));

    var result = fixture.client().register(invoice());

    assertThat(result.status()).isEqualTo("ERROR");
    assertThat(result.errorMessage()).contains("HTTP 400");
    fixture.server().verify();
  }

  @Test
  void queriesStatusThroughDocumentedPostEndpoint() throws Exception {
    var fixture = fixture(true);
    fixture
        .server()
        .expect(requestTo("https://sifphml.minfin.gov.ao/sigt/fe/v1/obterEstado"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", basicAuth("sandbox-user", "sandbox-password")))
        .andExpect(jsonPath("$.requestID").value("202600000010689"))
        .andExpect(jsonPath("$.jwsSignature").value("request-jws"))
        .andRespond(
            withSuccess(
                """
                  {"requestID":"202600000010689","resultCode":8,"documentStatusList":[],"requestErrorList":[]}
                  """,
                MediaType.APPLICATION_JSON));

    var result = fixture.client().checkStatus("202600000010689");

    assertThat(result.status()).isEqualTo("PENDING");
    fixture.server().verify();
  }

  private Fixture fixture(boolean enabled) throws Exception {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    var properties = new AgtProperties();
    properties.setApiUrl("https://sifphml.minfin.gov.ao");
    properties.setEnabled(enabled);
    properties.setUsername("sandbox-user");
    properties.setPassword("sandbox-password");
    properties.setNif("5000000000");
    properties.setSoftwareId("HospitalAO");
    properties.setSoftwareVersion("1.0.0");
    properties.setSoftwareValidationNumber("C_134");
    properties.setTaxExemptionCode("M03");

    var signingService = mock(AgtSigningService.class);
    when(signingService.signSoftware()).thenReturn("software-jws");
    when(signingService.signRequest(anyString())).thenReturn("request-jws");
    when(signingService.signDocument(
            anyString(),
            anyString(),
            anyString(),
            nullable(String.class),
            anyString(),
            anyString(),
            any(BigDecimal.class),
            any(BigDecimal.class),
            any(BigDecimal.class)))
        .thenReturn("document-jws");

    var client = new AgtApiClient(properties, signingService, new ObjectMapper(), builder);
    return new Fixture(client, server);
  }

  private Invoice invoice() {
    var patient = mock(Patient.class);
    when(patient.getFullName()).thenReturn("Paciente de Teste");
    var servicePrice = ServicePrice.builder().code("CONS-001").build();
    var item =
        InvoiceItem.builder()
            .servicePrice(servicePrice)
            .description("Consulta")
            .quantity(1)
            .unitPrice(new BigDecimal("100.00"))
            .discountPercent(BigDecimal.ZERO)
            .vatRate(new BigDecimal("14.00"))
            .lineTotal(new BigDecimal("114.00"))
            .build();

    return Invoice.builder()
        .invoiceNumber("FT 2026/0000001")
        .documentType(DocumentType.FT)
        .patient(patient)
        .patientNif("5000000001")
        .patientFiscalName("Paciente de Teste")
        .issuedAt(OffsetDateTime.parse("2026-10-07T12:00:00Z"))
        .subtotal(new BigDecimal("100.00"))
        .vatAmount(new BigDecimal("14.00"))
        .totalAmount(new BigDecimal("114.00"))
        .items(List.of(item))
        .build();
  }

  private String basicAuth(String username, String password) {
    return "Basic "
        + java.util.Base64.getEncoder()
            .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
  }

  private record Fixture(AgtApiClient client, MockRestServiceServer server) {}
}
