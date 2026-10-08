package ao.hospitalao.modules.telemedicine.provider;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "telemedicine.teams")
public class TeamsProperties {

  private boolean enabled;
  private String tenantId;
  private String clientId;
  private String clientSecret;

  @AssertTrue(message = "Teams credentials are required when the integration is enabled")
  public boolean isCredentialsConfiguredWhenEnabled() {
    return !enabled || hasCredentials();
  }

  public boolean hasCredentials() {
    return hasText(tenantId) && hasText(clientId) && hasText(clientSecret);
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
