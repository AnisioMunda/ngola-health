package ao.hospitalao.modules.financial.agt;

import ao.hospitalao.modules.financial.entity.Invoice;
import ao.hospitalao.modules.financial.entity.InvoiceItem;
import ao.hospitalao.modules.financial.util.FinancialAmounts;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
public class AgtApiClient {

  private static final String REGISTER_PATH = "/sigt/fe/v1/registarFactura";
  private static final String STATUS_PATH = "/sigt/fe/v1/obterEstado";
  private static final String SCHEMA_VERSION = "2.0";
  private static final String DOMESTIC_COUNTRY = "AO";
  private static final String UNIT_OF_MEASURE = "UN";
  private static final int MAX_ATTEMPTS = 3;
  private static final long INITIAL_RETRY_DELAY_MILLIS = 100;

  private final AgtProperties properties;
  private final AgtSigningService signingService;
  private final ObjectMapper objectMapper;
  private final RestClient restClient;

  public AgtApiClient(
      AgtProperties properties,
      AgtSigningService signingService,
      ObjectMapper objectMapper,
      RestClient.Builder restClientBuilder) {
    this.properties = properties;
    this.signingService = signingService;
    this.objectMapper = objectMapper;
    this.restClient =
        restClientBuilder
            .clone()
            .messageConverters(
                converters ->
                    converters.add(0, new MappingJackson2HttpMessageConverter(objectMapper)))
            .baseUrl(properties.getApiUrl())
            .defaultHeaders(
                headers -> headers.setBasicAuth(properties.getUsername(), properties.getPassword()))
            .build();
  }

  public AgtSubmissionResult register(Invoice invoice) throws Exception {
    if (!properties.isEnabled()) {
      return AgtSubmissionResult.error(
          "Integração AGT desactivada até à validação técnica e homologação.");
    }

    ObjectNode request = buildInvoicePayload(invoice);
    log.info("Submitting invoice {} to AGT", invoice.getInvoiceNumber());

    try {
      JsonNode response = postWithRetry(REGISTER_PATH, request);
      if (response == null || response.isNull()) {
        return AgtSubmissionResult.error("A AGT devolveu uma resposta vazia ao registo.");
      }
      if (response.path("errorList").isArray() && !response.path("errorList").isEmpty()) {
        return AgtSubmissionResult.error("A AGT devolveu erros de validação para a factura.");
      }

      String requestId = response.path("requestID").asText("");
      if (requestId.isBlank()) {
        return AgtSubmissionResult.error("A resposta da AGT não contém o identificador requestID.");
      }

      return AgtSubmissionResult.pending(requestId);
    } catch (RestClientResponseException exception) {
      return AgtSubmissionResult.error(
          "A API AGT respondeu com HTTP %d.".formatted(exception.getStatusCode().value()));
    } catch (ResourceAccessException exception) {
      return AgtSubmissionResult.error(
          "Não foi possível contactar a API AGT após %d tentativas.".formatted(MAX_ATTEMPTS));
    }
  }

  public AgtStatusResult checkStatus(String requestId) throws Exception {
    if (!properties.isEnabled()) {
      return new AgtStatusResult(
          "UNKNOWN", null, null, "Integração AGT desactivada até à validação técnica.");
    }
    if (requestId == null || requestId.isBlank()) {
      throw new IllegalArgumentException("requestID da AGT é obrigatório.");
    }

    ObjectNode request = buildCommonRequest();
    request.put("requestID", requestId);
    request.put("jwsSignature", signingService.signRequest(requestId));

    JsonNode response = postWithRetry(STATUS_PATH, request);
    if (response == null || response.isNull()) {
      return new AgtStatusResult("UNKNOWN", null, null, "A AGT devolveu uma resposta vazia.");
    }

    int resultCode = response.path("resultCode").asInt(-1);
    if (resultCode == 7) {
      return new AgtStatusResult(
          "PENDING", null, null, "A consulta foi prematura ou repetida; aguarde antes de repetir.");
    }
    if (resultCode == 8) {
      return new AgtStatusResult(
          "PENDING", null, null, "O processamento da AGT continua em curso.");
    }
    if (resultCode == 9) {
      return new AgtStatusResult(
          "REJECTED", null, null, "A AGT cancelou o processamento da solicitação.");
    }
    if (resultCode != 0 && resultCode != 1 && resultCode != 2) {
      return new AgtStatusResult(
          "UNKNOWN",
          null,
          null,
          "Código resultCode da AGT desconhecido: %d.".formatted(resultCode));
    }

    JsonNode statuses = response.path("documentStatusList");
    if (!statuses.isArray() || statuses.isEmpty()) {
      return new AgtStatusResult(
          "UNKNOWN", null, null, "A resposta da AGT não contém o estado da factura.");
    }

    JsonNode documentStatus = statuses.get(0);
    String status = documentStatus.path("documentStatus").asText("");
    if ("V".equals(status)) {
      return new AgtStatusResult("ACCEPTED", null, null, null);
    }
    if ("I".equals(status)) {
      return new AgtStatusResult(
          "REJECTED", null, null, documentErrorMessage(documentStatus.path("errorList")));
    }
    return new AgtStatusResult(
        "UNKNOWN", null, null, "Estado documentStatus da AGT desconhecido: %s.".formatted(status));
  }

  private String documentErrorMessage(JsonNode errors) {
    if (!errors.isArray() || errors.isEmpty()) {
      return "A AGT marcou a factura como inválida sem fornecer detalhes do erro.";
    }

    ArrayList<String> details = new ArrayList<>();
    for (JsonNode error : errors) {
      String code = error.path("errorCode").asText("");
      String description = error.path("errorDescription").asText("");
      if (code.isBlank() && description.isBlank()) {
        details.add("Erro AGT sem código ou descrição.");
      } else if (code.isBlank()) {
        details.add(description);
      } else if (description.isBlank()) {
        details.add(code);
      } else {
        details.add("%s: %s".formatted(code, description));
      }
    }

    String message = "A AGT marcou a factura como inválida: " + String.join("; ", details);
    return message.length() <= 500 ? message : message.substring(0, 497) + "...";
  }

  private ObjectNode buildInvoicePayload(Invoice invoice) throws Exception {
    if (invoice == null) {
      throw new IllegalArgumentException("Factura é obrigatória para submissão à AGT.");
    }
    if (invoice.getIssuedAt() == null) {
      throw new IllegalArgumentException(
          "A factura tem de estar emitida antes da submissão à AGT.");
    }
    if (invoice.getItems() == null || invoice.getItems().isEmpty()) {
      throw new IllegalArgumentException("A factura tem de conter pelo menos uma linha.");
    }
    requireConfigured(properties.getSoftwareValidationNumber(), "AGT_SOFTWARE_VALIDATION");

    ObjectNode request = buildCommonRequest();
    request.put("numberOfEntries", 1);

    ObjectNode document = objectMapper.createObjectNode();
    document.put("documentNo", invoice.getInvoiceNumber());
    document.put("documentStatus", "N");
    document.put(
        "jwsDocumentSignature",
        signingService.signDocument(
            invoice.getInvoiceNumber(),
            invoice.getDocumentType().name(),
            invoice.getIssuedAt().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
            invoice.getPatientNif(),
            DOMESTIC_COUNTRY,
            customerName(invoice),
            invoice.getVatAmount(),
            invoice.getSubtotal(),
            invoice.getTotalAmount()));
    document.put(
        "documentDate",
        invoice.getIssuedAt().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
    document.put("documentType", invoice.getDocumentType().name());
    document.put("systemEntryDate", OffsetDateTime.now(ZoneOffset.UTC).toString());
    document.put(
        "customerTaxID",
        invoice.getPatientNif() != null && !invoice.getPatientNif().isBlank()
            ? invoice.getPatientNif()
            : "999999999");
    document.put("customerCountry", DOMESTIC_COUNTRY);
    document.put("companyName", customerName(invoice));

    ObjectNode totals = document.putObject("documentTotals");
    totals.put("taxPayable", invoice.getVatAmount());
    totals.put("netTotal", invoice.getSubtotal());
    totals.put("grossTotal", invoice.getTotalAmount());

    ArrayNode lines = document.putArray("lines");
    BigDecimal taxTotal = BigDecimal.ZERO;
    int lineNumber = 1;
    for (InvoiceItem item : invoice.getItems()) {
      taxTotal = taxTotal.add(appendInvoiceLine(lines, item, lineNumber++));
    }
    if (FinancialAmounts.round(taxTotal).compareTo(FinancialAmounts.round(invoice.getVatAmount()))
        != 0) {
      throw new IllegalArgumentException(
          "O IVA arredondado pela regra AGT difere do total da factura; a submissão foi interrompida.");
    }

    request.putArray("documents").add(document);
    return request;
  }

  private ObjectNode buildCommonRequest() throws Exception {
    ObjectNode request = objectMapper.createObjectNode();
    request.put("schemaVersion", SCHEMA_VERSION);
    request.put("submissionUUID", UUID.randomUUID().toString());
    request.put("taxRegistrationNumber", properties.getNif());
    request.put("submissionTimeStamp", OffsetDateTime.now(ZoneOffset.UTC).toString());

    ObjectNode softwareInfo = request.putObject("softwareInfo");
    ObjectNode detail = softwareInfo.putObject("softwareInfoDetail");
    detail.put("productId", properties.getSoftwareId());
    detail.put("productVersion", properties.getSoftwareVersion());
    detail.put("softwareValidationNumber", properties.getSoftwareValidationNumber());
    softwareInfo.put("jwsSoftwareSignature", signingService.signSoftware());
    return request;
  }

  private BigDecimal appendInvoiceLine(ArrayNode lines, InvoiceItem item, int lineNumber) {
    if (item == null || item.getServicePrice() == null) {
      throw new IllegalArgumentException(
          "Cada linha tem de estar associada a um serviço com código AGT.");
    }
    String productCode = item.getServicePrice().getCode();
    if (productCode == null || productCode.isBlank()) {
      throw new IllegalArgumentException("O código do serviço é obrigatório para a AGT.");
    }
    if (item.getDescription() == null
        || item.getDescription().isBlank()
        || item.getDescription().length() > 200) {
      throw new IllegalArgumentException(
          "A descrição de cada linha tem de conter entre 1 e 200 caracteres.");
    }
    if (item.getDiscountPercent() != null && item.getDiscountPercent().signum() != 0) {
      throw new IllegalArgumentException(
          "Descontos por linha ainda não têm um mapeamento AGT validado.");
    }

    ObjectNode line = lines.addObject();
    line.put("lineNumber", lineNumber);
    line.put("productCode", productCode);
    line.put("productDescription", item.getDescription());
    line.put("quantity", item.getQuantity());
    line.put("unitOfMeasure", UNIT_OF_MEASURE);
    line.put("unitPriceBase", item.getUnitPrice());
    line.put("unitPrice", item.getUnitPrice());
    line.put("debitAmount", BigDecimal.ZERO);
    line.put("creditAmount", item.getNetAmount());

    ArrayNode taxes = line.putArray("taxes");
    ObjectNode tax = taxes.addObject();
    tax.put("taxType", "IVA");
    tax.put("taxCountryRegion", DOMESTIC_COUNTRY);
    BigDecimal rate = item.getVatRate() == null ? BigDecimal.ZERO : item.getVatRate();
    if (rate.compareTo(BigDecimal.valueOf(14)) == 0) {
      tax.put("taxCode", "NOR");
    } else if (rate.signum() == 0) {
      requireConfigured(properties.getTaxExemptionCode(), "AGT_TAX_EXEMPTION_CODE");
      tax.put("taxCode", "ISE");
      tax.put("taxExemptionCode", properties.getTaxExemptionCode());
    } else {
      throw new IllegalArgumentException(
          "A taxa IVA %s não tem um código fiscal AGT configurado.".formatted(rate));
    }
    tax.put("taxPercentage", rate);
    BigDecimal contribution = calculateAgtTaxContribution(item);
    tax.put("taxContribution", contribution);
    line.put("settlementAmount", BigDecimal.ZERO);
    return contribution;
  }

  private BigDecimal calculateAgtTaxContribution(InvoiceItem item) {
    BigDecimal rate = item.getVatRate() == null ? BigDecimal.ZERO : item.getVatRate();
    return item.getNetAmount()
        .multiply(rate)
        .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)
        .setScale(2, RoundingMode.CEILING);
  }

  private String customerName(Invoice invoice) {
    if (invoice.getPatientFiscalName() != null && !invoice.getPatientFiscalName().isBlank()) {
      return invoice.getPatientFiscalName();
    }
    if (invoice.getPatient() == null || invoice.getPatient().getFullName() == null) {
      throw new IllegalArgumentException("O nome fiscal do cliente é obrigatório para a AGT.");
    }
    return invoice.getPatient().getFullName();
  }

  private void requireConfigured(String value, String propertyName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(
          "%s tem de estar configurado antes de activar a integração AGT.".formatted(propertyName));
    }
  }

  private JsonNode postWithRetry(String path, ObjectNode body) {
    ResourceAccessException lastNetworkFailure = null;
    RestClientResponseException lastServerFailure = null;

    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        return restClient
            .post()
            .uri(path)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(JsonNode.class);
      } catch (ResourceAccessException exception) {
        lastNetworkFailure = exception;
      } catch (RestClientResponseException exception) {
        if (!isRetryable(exception.getStatusCode().value())) {
          throw exception;
        }
        lastServerFailure = exception;
      }

      if (attempt < MAX_ATTEMPTS) {
        waitBeforeRetry(attempt);
      }
    }

    if (lastServerFailure != null) {
      throw lastServerFailure;
    }
    throw lastNetworkFailure;
  }

  private boolean isRetryable(int statusCode) {
    return statusCode >= 500;
  }

  private void waitBeforeRetry(int attempt) {
    long delay = INITIAL_RETRY_DELAY_MILLIS << (attempt - 1);
    try {
      Thread.sleep(delay);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("A repetição da chamada AGT foi interrompida.", exception);
    }
  }

  public record AgtSubmissionResult(String requestId, String status, String errorMessage) {
    public static AgtSubmissionResult pending(String requestId) {
      return new AgtSubmissionResult(requestId, "PENDING", null);
    }

    public static AgtSubmissionResult error(String message) {
      return new AgtSubmissionResult(null, "ERROR", message);
    }
  }

  public record AgtStatusResult(
      String status, String validationCode, String qrCode, String message) {}
}
