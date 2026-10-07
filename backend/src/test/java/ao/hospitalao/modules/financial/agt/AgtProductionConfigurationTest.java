package ao.hospitalao.modules.financial.agt;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AgtProductionConfigurationTest {

  @TempDir Path temporaryDirectory;

  @Test
  void refusesProductionWithoutExternalCredentialsAndMountedKeyPath() {
    var properties = validProperties();
    properties.setClientId("");
    properties.setPrivateKeyPath("");

    assertThatThrownBy(
            () ->
                AgtProductionConfiguration.validateProductionConfiguration(
                    properties, mock(AgtSigningService.class)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("AGT_CLIENT_ID")
        .hasMessageContaining("AGT_PRIVATE_KEY_PATH");
  }

  @Test
  void loadsConfiguredRsaPrivateKeyFromExternalFileDuringProductionValidation() throws Exception {
    var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048);
    var privateKey = keyPairGenerator.generateKeyPair().getPrivate();
    Path privateKeyFile = temporaryDirectory.resolve("agt-private-key.pem");
    String pem =
        "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(privateKey.getEncoded())
            + "\n-----END PRIVATE KEY-----\n";
    Files.writeString(privateKeyFile, pem);

    var properties = validProperties();
    properties.setPrivateKeyPath(privateKeyFile.toString());
    var signingService = new AgtSigningService(properties, new ObjectMapper());

    AgtProductionConfiguration.validateProductionConfiguration(properties, signingService);
  }

  @Test
  void failsProductionValidationWhenMountedKeyFileCannotBeLoaded() {
    var properties = validProperties();
    properties.setPrivateKeyPath(temporaryDirectory.resolve("missing.pem").toString());
    var signingService = new AgtSigningService(properties, new ObjectMapper());

    assertThatThrownBy(
            () ->
                AgtProductionConfiguration.validateProductionConfiguration(
                    properties, signingService))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("The AGT signing key file is unavailable or invalid.");
  }

  private AgtProperties validProperties() {
    var properties = new AgtProperties();
    properties.setClientId("test-client-id");
    properties.setClientSecret("test-client-secret");
    properties.setPrivateKeyPath("/run/secrets/agt-private-key.pem");
    return properties;
  }
}
