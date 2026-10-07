package ao.hospitalao.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
    @NotBlank
    @Size(min = 44, message = "must decode to at least 256 bits")
    @Pattern(
        regexp = "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$",
        message = "must be a valid Base64-encoded key"
    )
    String secret,
    @Positive
    long expirationMs,
    @Positive
    long refreshExpirationMs
) {
}
