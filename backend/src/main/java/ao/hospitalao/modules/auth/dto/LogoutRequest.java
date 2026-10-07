package ao.hospitalao.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogoutRequest {

  @NotBlank(message = "O refresh token é obrigatório")
  private String refreshToken;
}
