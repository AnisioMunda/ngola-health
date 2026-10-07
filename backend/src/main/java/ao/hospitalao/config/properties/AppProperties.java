package ao.hospitalao.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
    @NotBlank @Pattern(regexp = "https?://[^\\s]+", message = "must be an absolute HTTP(S) URL")
        String baseUrl) {}
