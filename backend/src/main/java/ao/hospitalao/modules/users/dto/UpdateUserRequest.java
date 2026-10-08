package ao.hospitalao.modules.users.dto;

import jakarta.validation.constraints.*;
import java.util.Set;
import java.util.UUID;
import lombok.Data;

@Data
public class UpdateUserRequest {

  @Size(min = 3, max = 200)
  private String fullName;

  @Email(message = "Invalid email format")
  private String email;

  private String phone;
  private String especiality;
  private String professionalCard;

  @Pattern(
      regexp = "^$|^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
      message = "Microsoft Entra user ID must be a UUID")
  private String teamsUserId;

  // Null = não alterar os roles
  private Set<UUID> roleIds;
}
