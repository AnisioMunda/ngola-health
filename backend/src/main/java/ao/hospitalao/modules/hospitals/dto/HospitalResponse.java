package ao.hospitalao.modules.hospitals.dto;

import ao.hospitalao.modules.hospitals.entity.Hospital.HospitalType;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HospitalResponse {
  private UUID id;
  private String name;
  private String code;
  private HospitalType type;
  private String province;
  private String municipality;
  private String address;
  private String phone;
  private String email;
  private String taxId;
  private boolean active;
}
