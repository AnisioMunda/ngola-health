package ao.hospitalao.modules.users.dto;

import ao.hospitalao.modules.auth.entity.enums.RegisterStatus;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
  private UUID id;
  private String fullName;
  private String username;
  private String email;
  private String phone;
  private String especiality;
  private String professionalCard;
  private RegisterStatus registerStatus;
  private boolean mustChangePassword;
  private OffsetDateTime lastLogin;
  private OffsetDateTime createdAt;
  private Set<String> roles;
  private UUID hospitalId;
}
