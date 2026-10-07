package ao.hospitalao.modules.financial.agt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "agt")
public class AgtProperties {

  @NotBlank
  @Pattern(regexp = "https?://[^\\s]+", message = "must be an absolute HTTP(S) URL")
  private String apiUrl;

  private String clientId;
  private String clientSecret;
  @NotBlank private String nif;
  @NotBlank private String softwareId;
  @NotBlank private String softwareVersion;
  private String softwareValidationNumber;
  private String privateKeyPath;
  private String privateKeyContent;
  private boolean sandbox;
}
