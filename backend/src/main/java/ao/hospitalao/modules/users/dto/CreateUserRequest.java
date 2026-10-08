package ao.hospitalao.modules.users.dto;

import jakarta.validation.constraints.*;
import java.util.Set;
import java.util.UUID;
import lombok.Data;

@Data
public class CreateUserRequest {

  @NotBlank(message = "Full name is required")
  @Size(min = 3, max = 200, message = "Full name must be between 3 and 200 characters")
  private String fullName;

  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
  @Pattern(
      regexp = "^[a-z0-9._-]+$",
      message =
          "Username can only contain lowercase letters, numbers, dots, underscores and hyphens")
  private String username;

  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email format")
  private String email;

  @NotBlank(message = "Password is required")
  @Size(min = 8, max = 100)
  @Pattern(
      regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
      message = "Password must contain uppercase, lowercase and numbers")
  private String password;

  private String phone;
  private String especiality;
  private String professionalCard;

  @Pattern(
      regexp = "^$|^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
      message = "Microsoft Entra user ID must be a UUID")
  private String teamsUserId;

  @NotEmpty(message = "At least one role is required")
  private Set<UUID> roleIds;

  private boolean mustChangePassword = true;
}
