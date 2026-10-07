package ao.hospitalao.modules.financial.agt;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("prod")
public class AgtProductionConfiguration {

  @Bean
  InitializingBean validateAgtProductionSecrets(
      AgtProperties properties, AgtSigningService signingService) {
    return () -> validateProductionConfiguration(properties, signingService);
  }

  static void validateProductionConfiguration(
      AgtProperties properties, AgtSigningService signingService) {
    if (!properties.isEnabled()) {
      return;
    }

    List<String> missing = new ArrayList<>();
    if (isBlank(properties.getClientId())) {
      missing.add("AGT_CLIENT_ID");
    }
    if (isBlank(properties.getClientSecret())) {
      missing.add("AGT_CLIENT_SECRET");
    }
    if (isBlank(properties.getPrivateKeyPath())) {
      missing.add("AGT_PRIVATE_KEY_PATH");
    }
    if (!missing.isEmpty()) {
      throw new IllegalStateException(
          "Production AGT integration requires externally supplied configuration: "
              + String.join(", ", missing));
    }

    signingService.validatePrivateKey();
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
