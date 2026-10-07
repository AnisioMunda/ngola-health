package ao.hospitalao.modules.patients.dto;

import ao.hospitalao.modules.patients.entity.Patient.Gender;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PatientResponse {
  private UUID id;
  private String fullName;
  private LocalDate birthDate;
  private int age;
  private Gender gender;
  private String nationalId;
  private String healthCardNumber;
  private String phone;
  private String email;
  private String address;
  private String province;
  private String municipality;
  private String emergencyContactName;
  private String emergencyContactPhone;
  private String emergencyContactRelationship;
  private String bloodType;
  private String allergies;
  private String chronicConditions;
  private String notes;
  private boolean active;
  private OffsetDateTime createdAt;
}
