package ao.hospitalao.modules.hospitals.dto;

import ao.hospitalao.modules.hospitals.entity.Hospital.HospitalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateHospitalRequest {

  @NotBlank(message = "Name is required")
  private String name;

  @NotBlank(message = "Code is required")
  @Pattern(
      regexp = "^[A-Z0-9-]{3,20}$",
      message = "Code must be 3-20 uppercase letters, numbers or hyphens")
  private String code;

  @NotNull(message = "Type is required")
  private HospitalType type;

  @NotBlank(message = "Province is required")
  private String province;

  private String municipality;
  private String address;
  private String phone;
  private String email;
  private String taxId;
}
