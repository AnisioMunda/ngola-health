package ao.hospitalao.modules.financial.agt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Serviço responsável pela assinatura digital JWS RS256 conforme exigido pela API AGT Angola.
 *
 * <p>Gera as 3 assinaturas obrigatórias: 1. jwsSoftwareSignature — identidade do software 2.
 * jwsDocumentSignature — integridade do documento fiscal 3. jwsSignature — autenticidade da
 * requisição
 *
 * <p>Documentação: quiosqueagt.minfin.gov.ao/doc-agt/faturacao-electronica/1/estrutura.html
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgtSigningService {

  private final AgtProperties properties;

  @SuppressWarnings("unused")
  private final ObjectMapper objectMapper;

  private PrivateKey cachedPrivateKey;

  // ------------------------------------------------
  // 1. jwsSoftwareSignature
  // ------------------------------------------------

  /**
   * Assina os dados do software de facturação. Payload: { productId, productVersion,
   * softwareValidationNumber }
   */
  public String signSoftware() throws Exception {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("productId", properties.getSoftwareId());
    payload.put("productVersion", properties.getSoftwareVersion());
    payload.put("softwareValidationNumber", properties.getSoftwareValidationNumber());

    return sign(payload);
  }

  // ------------------------------------------------
  // 2. jwsDocumentSignature
  // ------------------------------------------------

  /**
   * Assina os campos fiscais obrigatórios do documento. Payload conforme especificação AGT: {
   * documentNo, taxRegistrationNumber, documentType, documentDate, customerTaxID, customerCountry,
   * companyName, documentTotals }
   */
  public String signDocument(
      String documentNo,
      String documentType,
      String documentDate,
      String customerTaxId,
      String customerCountry,
      String companyName,
      java.math.BigDecimal taxPayable,
      java.math.BigDecimal netTotal,
      java.math.BigDecimal grossTotal)
      throws Exception {
    Map<String, Object> totals = new LinkedHashMap<>();
    totals.put("taxPayable", taxPayable);
    totals.put("netTotal", netTotal);
    totals.put("grossTotal", grossTotal);

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("documentNo", documentNo);
    payload.put("taxRegistrationNumber", properties.getNif());
    payload.put("documentType", documentType);
    payload.put("documentDate", documentDate);
    payload.put("customerTaxID", customerTaxId != null ? customerTaxId : "999999999");
    payload.put("customerCountry", customerCountry != null ? customerCountry : "AO");
    payload.put("companyName", companyName);
    payload.put("documentTotals", totals);

    return sign(payload);
  }

  // ------------------------------------------------
  // 3. jwsSignature (assinatura da requisição)
  // ------------------------------------------------

  /**
   * Assina o objecto principal da requisição à API AGT. Payload: { taxRegistrationNumber, requestID
   * }
   */
  public String signRequest(String requestId) throws Exception {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("taxRegistrationNumber", properties.getNif());
    payload.put("requestID", requestId);

    return sign(payload);
  }

  // ------------------------------------------------
  // Assinatura JWS RS256
  // ------------------------------------------------

  @SuppressWarnings("deprecation")
  private String sign(Map<String, Object> payload) throws Exception {
    PrivateKey privateKey = getPrivateKey();

    return Jwts.builder()
        .setClaims(payload)
        .signWith(privateKey, SignatureAlgorithm.RS256)
        .compact();
  }

  // ------------------------------------------------
  // Carregamento da chave privada RSA
  // ------------------------------------------------

  private PrivateKey getPrivateKey() throws Exception {
    if (cachedPrivateKey != null) return cachedPrivateKey;

    String pemContent;

    // Prioridade: conteúdo directo > ficheiro
    if (properties.getPrivateKeyContent() != null && !properties.getPrivateKeyContent().isBlank()) {
      pemContent = properties.getPrivateKeyContent();
    } else if (properties.getPrivateKeyPath() != null) {
      pemContent =
          java.nio.file.Files.readString(java.nio.file.Path.of(properties.getPrivateKeyPath()));
    } else {
      throw new IllegalStateException(
          "AGT private key not configured. Set agt.private-key-content or agt.private-key-path");
    }

    cachedPrivateKey = parsePemPrivateKey(pemContent);
    return cachedPrivateKey;
  }

  private PrivateKey parsePemPrivateKey(String pem) throws Exception {
    // Remover headers PEM e espaços/newlines
    String cleaned =
        pem.replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s+", "");

    byte[] keyBytes = Base64.getDecoder().decode(cleaned);
    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
    KeyFactory kf = KeyFactory.getInstance("RSA");
    return kf.generatePrivate(spec);
  }
}
