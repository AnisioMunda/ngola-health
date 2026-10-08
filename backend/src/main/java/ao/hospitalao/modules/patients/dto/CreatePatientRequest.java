package ao.hospitalao.modules.patients.dto;

import ao.hospitalao.modules.patients.entity.Patient.Gender;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import lombok.Data;

@Data
public class CreatePatientRequest {

  @NotBlank(message = "O nome completo é obrigatório.")
  @Size(min = 3, max = 200, message = "O nome deve ter entre 3 e 200 caracteres.")
  private String fullName;

  @NotNull(message = "A data de nascimento é obrigatória.")
  @Past(message = "A data de nascimento deve ser anterior à data actual.")
  private LocalDate birthDate;

  @NotNull(message = "O género é obrigatório.")
  private Gender gender;

  @Size(max = 20, message = "O BI/NIF não pode exceder 20 caracteres.")
  @Pattern(
      regexp = "(?i)^(?:|\\d{9}[a-z]{2}\\d{3}|\\d{10})$",
      message = "O BI/NIF deve ter um formato válido.")
  private String nationalId;

  @Size(max = 30, message = "O número do cartão de saúde não pode exceder 30 caracteres.")
  private String healthCardNumber;

  @Size(max = 20, message = "O telefone não pode exceder 20 caracteres.")
  private String phone;

  @Email(message = "O endereço de correio electrónico deve ser válido.")
  @Size(max = 200, message = "O endereço de correio electrónico não pode exceder 200 caracteres.")
  private String email;

  @Size(max = 500, message = "A morada não pode exceder 500 caracteres.")
  private String address;

  @Size(max = 100, message = "A província não pode exceder 100 caracteres.")
  private String province;

  @Size(max = 100, message = "O município não pode exceder 100 caracteres.")
  private String municipality;

  @Size(max = 200, message = "O nome do contacto de emergência não pode exceder 200 caracteres.")
  private String emergencyContactName;

  @Size(max = 20, message = "O telefone do contacto de emergência não pode exceder 20 caracteres.")
  private String emergencyContactPhone;

  @Size(max = 50, message = "O parentesco não pode exceder 50 caracteres.")
  private String emergencyContactRelationship;

  @Size(max = 5, message = "O grupo sanguíneo não pode exceder 5 caracteres.")
  private String bloodType;

  private String allergies;
  private String chronicConditions;
  private String notes;
}
