package ao.hospitalao.modules.financial.agt;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Creates compact JWS signatures with RS256. The AGT protocol and payload schemas remain
 * unverified; see ADR-0009 before treating this signing format as AGT-compliant.
 *
 * <p>The payloads built by the public methods are provisional and must be confirmed against the
 * official partner documentation before they are sent to AGT.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgtSigningService {

  private static final byte[] PROTECTED_HEADER =
      "{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8);

  private final AgtProperties properties;

  private final ObjectMapper objectMapper;

  private PrivateKey cachedPrivateKey;

  // ------------------------------------------------
  // 1. jwsSoftwareSignature
  // ------------------------------------------------

  /** Signs the current provisional software-identification payload. */
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
   * Signs the current provisional document payload. Its field names and contents are not confirmed
   * as an AGT contract.
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

  /** Signs the current provisional request payload. */
  public String signRequest(String requestId) throws Exception {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("taxRegistrationNumber", properties.getNif());
    payload.put("requestID", requestId);

    return sign(payload);
  }

  // ------------------------------------------------
  // Assinatura JWS RS256
  // ------------------------------------------------

  private String sign(Map<String, Object> payload) throws Exception {
    return signPayload(objectMapper.writeValueAsBytes(payload), getPrivateKey());
  }

  static String signPayload(byte[] payload, PrivateKey privateKey) throws GeneralSecurityException {
    return signPayload(PROTECTED_HEADER, payload, privateKey);
  }

  static String signPayload(byte[] header, byte[] payload, PrivateKey privateKey)
      throws GeneralSecurityException {
    Objects.requireNonNull(header, "header must not be null");
    Objects.requireNonNull(payload, "payload must not be null");
    Objects.requireNonNull(privateKey, "privateKey must not be null");

    String signingInput =
        Base64.getUrlEncoder().withoutPadding().encodeToString(header)
            + "."
            + Base64.getUrlEncoder().withoutPadding().encodeToString(payload);

    Signature signer = Signature.getInstance("SHA256withRSA");
    signer.initSign(privateKey);
    signer.update(signingInput.getBytes(StandardCharsets.US_ASCII));
    String encodedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signer.sign());
    return signingInput + "." + encodedSignature;
  }

  // ------------------------------------------------
  // Carregamento da chave privada RSA
  // ------------------------------------------------

  private PrivateKey getPrivateKey() throws Exception {
    if (cachedPrivateKey != null) return cachedPrivateKey;

    String privateKeyPath = properties.getPrivateKeyPath();
    if (privateKeyPath == null || privateKeyPath.isBlank()) {
      throw new IllegalStateException("AGT_PRIVATE_KEY_PATH is not configured.");
    }

    String pemContent = java.nio.file.Files.readString(java.nio.file.Path.of(privateKeyPath));
    cachedPrivateKey = parsePemPrivateKey(pemContent);
    return cachedPrivateKey;
  }

  public void validatePrivateKey() {
    try {
      getPrivateKey();
    } catch (Exception exception) {
      throw new IllegalStateException(
          "The AGT signing key file is unavailable or invalid.", exception);
    }
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
