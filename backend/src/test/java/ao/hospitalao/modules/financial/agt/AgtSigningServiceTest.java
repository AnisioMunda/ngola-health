package ao.hospitalao.modules.financial.agt;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.RSAPrivateKeySpec;
import java.util.Base64;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AgtSigningServiceTest {

  private static final String RFC7515_RSA_MODULUS =
      "ofgWCuLjybRlzo0tZWJjNiuSfb4p4fAkd_wWJcyQoTbji9k0l8W26mPddx"
          + "HmfHQp-Vaw-4qPCJrcS2mJPMEzP1Pt0Bm4d4QlL-yRT-SFd2lZS-pCgNMs"
          + "D1W_YpRPEwOWvG6b32690r2jZ47soMZo9wGzjb_7OMg0LOL-bSf63kpaSH"
          + "SXndS5z5rexMdbBYUsLA9e-KXBdQOS-UTo7WTBEMa2R2CapHg665xsmtdV"
          + "MTBQY4uDZlxvb3qCo5ZwKh9kG4LT6_I5IhlJH7aGhyxXFvUK-DWNmoudF8"
          + "NAco9_h9iaGNj8q2ethFkMLs91kzk2PAcDTW9gb54h4FRWyuXpoQ";

  private static final String RFC7515_RSA_PRIVATE_EXPONENT =
      "Eq5xpGnNCivDflJsRQBXHx1hdR1k6Ulwe2JZD50LpXyWPEAeP88vLNO97I"
          + "jlA7_GQ5sLKMgvfTeXZx9SE-7YwVol2NXOoAJe46sui395IW_GO-pWJ1O0"
          + "BkTGoVEn2bKVRUCgu-GjBVaYLU6f3l9kJfFNS3E0QbVdxzubSu3Mkqzjkn"
          + "439X0M_V51gfpRLI9JYanrC4D4qAdGcopV_0ZHHzQlBjudU2QvXt4ehNYT"
          + "CBr6XCLQUShb1juUO1ZdiYoFaFQT5Tw8bGUl_x_jTj3ccPDVZFD9pIuhLh"
          + "BOneufuBiB4cS98l2SR_RQyGWSeWjnczT0QU91p1DhOVRuOopznQ";

  @TempDir Path temporaryDirectory;

  @Test
  void signsRfc7515Rs256KnownVector() throws Exception {
    byte[] payload =
        ("{\"iss\":\"joe\",\r\n"
                + " \"exp\":1300819380,\r\n"
                + " \"http://example.com/is_root\":true}")
            .getBytes(StandardCharsets.UTF_8);

    String token = AgtSigningService.signPayload(payload, rfc7515PrivateKey());

    assertThat(token)
        .isEqualTo(
            "eyJhbGciOiJSUzI1NiJ9."
                + "eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFt"
                + "cGxlLmNvbS9pc19yb290Ijp0cnVlfQ."
                + "cC4hiUPoj9Eetdgtv3hF80EGrhuB__dzERat0XF9g2VtQgr9PJbu3XOiZj5RZmh7"
                + "AAuHIm4Bh-0Qc_lF5YKt_O8W2Fp5jujGbds9uJdbF9CUAr7t1dnZcAcQjbKBYNX4"
                + "BAynRFdiuB--f_nZLgrnbyTyWzO75vRK5h6xBArLIARNPvkSjtQBMHlb1L07Qe7K"
                + "0GarZRmB_eSN9383LcOLn6_dO--xi12jzDwusC-eOkHWEsqtFZESc6BfI7noOPqv"
                + "hJ1phCnvWh6IeYI2w9QOYEUipUTI8np6LbgGY9Fs98rqVt5AXLIhWkWywlVmtVrB"
                + "p0igcN_IoypGlUPQGe77Rw");
  }

  @Test
  @SuppressWarnings("deprecation")
  void signsRequestPayloadThatCanBeVerifiedWithItsRsaPublicKey() throws Exception {
    var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048);
    var keyPair = keyPairGenerator.generateKeyPair();
    Path privateKeyFile = temporaryDirectory.resolve("signing-key.pem");
    String pem =
        "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getMimeEncoder(64, new byte[] {'\n'})
                .encodeToString(keyPair.getPrivate().getEncoded())
            + "\n-----END PRIVATE KEY-----\n";
    Files.writeString(privateKeyFile, pem);

    var properties = new AgtProperties();
    properties.setPrivateKeyPath(privateKeyFile.toString());
    properties.setNif("5000000000");
    var signingService = new AgtSigningService(properties, new ObjectMapper());

    String[] token = signingService.signRequest("request-123").split("\\.", -1);
    var expectedClaims = new LinkedHashMap<String, Object>();
    expectedClaims.put("taxRegistrationNumber", "5000000000");
    expectedClaims.put("requestID", "request-123");
    String legacyToken =
        Jwts.builder()
            .setClaims(expectedClaims)
            .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
            .compact();
    byte[] signingInput = (token[0] + "." + token[1]).getBytes(StandardCharsets.US_ASCII);
    var verifier = Signature.getInstance("SHA256withRSA");
    verifier.initVerify(keyPair.getPublic());
    verifier.update(signingInput);

    assertThat(token).hasSize(3);
    assertThat(token[0]).isEqualTo("eyJhbGciOiJSUzI1NiJ9");
    assertThat(String.join(".", token)).isEqualTo(legacyToken);
    assertThat(verifier.verify(Base64.getUrlDecoder().decode(token[2]))).isTrue();
    var claims = new ObjectMapper().readTree(Base64.getUrlDecoder().decode(token[1]));
    assertThat(claims.get("taxRegistrationNumber").asText()).isEqualTo("5000000000");
    assertThat(claims.get("requestID").asText()).isEqualTo("request-123");
  }

  private RSAPrivateKey rfc7515PrivateKey() throws Exception {
    var modulus = new BigInteger(1, Base64.getUrlDecoder().decode(RFC7515_RSA_MODULUS));
    var privateExponent =
        new BigInteger(1, Base64.getUrlDecoder().decode(RFC7515_RSA_PRIVATE_EXPONENT));
    return (RSAPrivateKey)
        KeyFactory.getInstance("RSA")
            .generatePrivate(new RSAPrivateKeySpec(modulus, privateExponent));
  }
}
