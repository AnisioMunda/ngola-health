package ao.hospitalao.modules.patients.dto;

import ao.hospitalao.modules.patients.entity.Patient.Gender;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreatePatientRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 200)
    private String fullName;

    @NotNull(message = "Birth date is required")
    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;

    @NotNull(message = "Gender is required")
    private Gender gender;

    private String nationalId;
    private String healthCardNumber;

    // Contact
    private String phone;
    private String email;
    private String address;
    private String province;
    private String municipality;

    // Emergency contact
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;

    // Clinical
    private String bloodType;
    private String allergies;
    private String chronicConditions;
    private String notes;
}