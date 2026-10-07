package ao.hospitalao.modules.financial.agt;

import ao.hospitalao.modules.financial.entity.Invoice;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Cliente HTTP para a API AGT Angola. Suporta sandbox (portaldoparceiro.minfin.gov.ao) e produção.
 *
 * <p>Fluxo assíncrono: 1. POST /registar-fatura → recebe requestID 2. Gravar requestID na invoice
 * 3. GET /consultar-estado/{requestID} → ACEITE ou REJEITADO
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgtApiClient {

  private final AgtProperties properties;
  private final AgtSigningService signingService;
  private final ObjectMapper objectMapper;
  private final RestTemplate restTemplate;

  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  // ------------------------------------------------
  // Registar Factura Electrónica
  // POST /api/v1/invoices/register
  // ------------------------------------------------

  @SuppressWarnings("null")
  public AgtSubmissionResult register(Invoice invoice) throws Exception {
    String requestId = "REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

    // Construir payload conforme especificação AGT
    ObjectNode body = buildInvoicePayload(invoice, requestId);

    HttpHeaders headers = buildHeaders();
    HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

    String url = properties.getApiUrl() + "/api/v1/invoices/register";

    log.info("Submitting invoice {} to AGT. requestID: {}", invoice.getInvoiceNumber(), requestId);

    try {
      ResponseEntity<JsonNode> response =
          restTemplate.exchange(url, HttpMethod.POST, entity, JsonNode.class);

      if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        String agtRequestId = response.getBody().path("requestID").asText(requestId);
        log.info(
            "Invoice {} accepted by AGT. requestID: {}", invoice.getInvoiceNumber(), agtRequestId);
        return AgtSubmissionResult.pending(agtRequestId);
      } else {
        log.error("AGT rejected invoice {}: {}", invoice.getInvoiceNumber(), response.getBody());
        return AgtSubmissionResult.error("AGT returned: " + response.getStatusCode());
      }
    } catch (Exception e) {
      log.error(
          "Error submitting invoice {} to AGT: {}", invoice.getInvoiceNumber(), e.getMessage());
      return AgtSubmissionResult.error(e.getMessage());
    }
  }

  // ------------------------------------------------
  // Consultar Estado da Submissão (Polling)
  // GET /api/v1/invoices/status/{requestID}
  // ------------------------------------------------

  @SuppressWarnings("null")
  public AgtStatusResult checkStatus(String requestId) {
    String url = properties.getApiUrl() + "/api/v1/invoices/status/" + requestId;

    HttpHeaders headers = buildHeaders();
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<JsonNode> response =
          restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

      if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        JsonNode body = response.getBody();
        String status = body.path("status").asText();
        String validationCode = body.path("validationCode").asText(null);
        String qrCode = body.path("qrCode").asText(null);
        String message = body.path("message").asText(null);

        return new AgtStatusResult(status, validationCode, qrCode, message);
      }
    } catch (Exception e) {
      log.error("Error checking AGT status for {}: {}", requestId, e.getMessage());
    }

    return new AgtStatusResult("UNKNOWN", null, null, "Error checking status");
  }

  // ------------------------------------------------
  // Solicitar Série
  // POST /api/v1/series/request
  // ------------------------------------------------

  @SuppressWarnings("null")
  public String requestSeries(String documentType, int year) throws Exception {
    ObjectNode body = objectMapper.createObjectNode();
    body.put("taxRegistrationNumber", properties.getNif());
    body.put("documentType", documentType);
    body.put("year", year);
    body.put("jwsSignature", signingService.signRequest("SERIES-" + documentType + "-" + year));

    HttpHeaders headers = buildHeaders();
    HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

    String url = properties.getApiUrl() + "/api/v1/series/request";

    ResponseEntity<JsonNode> response =
        restTemplate.exchange(url, HttpMethod.POST, entity, JsonNode.class);

    if (response.getBody() != null) {
      return response.getBody().path("seriesCode").asText();
    }
    throw new RuntimeException("Failed to obtain series from AGT");
  }

  // ------------------------------------------------
  // Build payload conforme especificação AGT
  // ------------------------------------------------

  private ObjectNode buildInvoicePayload(Invoice invoice, String requestId) throws Exception {
    ObjectNode body = objectMapper.createObjectNode();

    // Dados do contribuinte
    body.put("taxRegistrationNumber", properties.getNif());
    body.put("requestID", requestId);

    // Dados do documento fiscal
    ObjectNode doc = objectMapper.createObjectNode();
    doc.put("documentNo", invoice.getInvoiceNumber());
    doc.put("documentType", invoice.getDocumentType().name());
    doc.put(
        "documentDate",
        invoice.getIssuedAt() != null
            ? invoice.getIssuedAt().format(DATE_FMT)
            : java.time.OffsetDateTime.now().format(DATE_FMT));

    // Dados do cliente
    doc.put(
        "customerTaxID", invoice.getPatientNif() != null ? invoice.getPatientNif() : "999999999");
    doc.put(
        "customerName",
        invoice.getPatientFiscalName() != null
            ? invoice.getPatientFiscalName()
            : invoice.getPatient().getFullName());
    doc.put("customerCountry", "AO");

    // Totais (campos obrigatórios AGT)
    ObjectNode totals = objectMapper.createObjectNode();
    totals.put("taxPayable", invoice.getVatAmount());
    totals.put("netTotal", invoice.getSubtotal());
    totals.put("grossTotal", invoice.getTotalAmount());
    doc.set("documentTotals", totals);

    // Linhas do documento
    var itemsArray = objectMapper.createArrayNode();
    for (var item : invoice.getItems()) {
      ObjectNode line = objectMapper.createObjectNode();
      line.put("description", item.getDescription());
      line.put("quantity", item.getQuantity());
      line.put("unitPrice", item.getUnitPrice());
      line.put("taxRate", item.getVatRate());
      line.put("lineTotal", item.getLineTotal());
      itemsArray.add(line);
    }
    doc.set("lines", itemsArray);
    body.set("document", doc);

    // Assinaturas JWS obrigatórias
    body.put("jwsSoftwareSignature", signingService.signSoftware());
    body.put(
        "jwsDocumentSignature",
        signingService.signDocument(
            invoice.getInvoiceNumber(),
            invoice.getDocumentType().name(),
            invoice.getIssuedAt() != null
                ? invoice.getIssuedAt().format(DATE_FMT)
                : java.time.OffsetDateTime.now().format(DATE_FMT),
            invoice.getPatientNif(),
            "AO",
            invoice.getPatientFiscalName() != null
                ? invoice.getPatientFiscalName()
                : invoice.getPatient().getFullName(),
            invoice.getVatAmount(),
            invoice.getSubtotal(),
            invoice.getTotalAmount()));
    body.put("jwsSignature", signingService.signRequest(requestId));

    return body;
  }

  private HttpHeaders buildHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Client-ID", properties.getClientId());
    headers.set("X-Client-Secret", properties.getClientSecret());
    headers.set("X-NIF", properties.getNif());
    return headers;
  }

  // ------------------------------------------------
  // Result classes
  // ------------------------------------------------

  public record AgtSubmissionResult(String requestId, String status, String errorMessage) {
    public static AgtSubmissionResult pending(String requestId) {
      return new AgtSubmissionResult(requestId, "PENDING", null);
    }

    public static AgtSubmissionResult error(String msg) {
      return new AgtSubmissionResult(null, "ERROR", msg);
    }
  }

  public record AgtStatusResult(
      String status, String validationCode, String qrCode, String message) {}
}
