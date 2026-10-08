package ao.hospitalao.modules.patients.dto;

import java.time.LocalDate;
import java.util.UUID;

public record PatientDuplicateCandidateResponse(UUID id, String fullName, LocalDate birthDate) {}
