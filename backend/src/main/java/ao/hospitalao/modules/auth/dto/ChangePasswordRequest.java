package ao.hospitalao.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Pedido para alterar a senha da conta autenticada")
public class ChangePasswordRequest {

  @NotBlank
  @Schema(description = "Senha actual", example = "SenhaTemporaria123")
  private String currentPassword;

  @NotBlank
  @Size(min = 12, max = 72, message = "A nova senha deve ter entre 12 e 72 caracteres")
  @Schema(description = "Nova senha", example = "UmaSenhaForte123")
  private String newPassword;
}
