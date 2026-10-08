package ao.hospitalao.modules.patients.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CheckPatientDuplicatesRequest(
    @NotBlank(message = "O nome completo é obrigatório.")
        @Size(max = 200, message = "O nome não pode exceder 200 caracteres.")
        String fullName,
    @NotNull(message = "A data de nascimento é obrigatória.")
        @Past(message = "A data de nascimento deve ser anterior à data actual.")
        LocalDate birthDate,
    @Size(max = 20, message = "O telefone não pode exceder 20 caracteres.") String phone) {}
