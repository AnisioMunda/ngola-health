package ao.hospitalao.modules.patients.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.patients.dto.CheckPatientDuplicatesRequest;
import ao.hospitalao.modules.patients.dto.CreatePatientRequest;
import ao.hospitalao.modules.patients.dto.PatientDuplicateCandidateResponse;
import ao.hospitalao.modules.patients.dto.PatientResponse;
import ao.hospitalao.modules.patients.entity.Patient;
import ao.hospitalao.modules.patients.exception.PatientIdentifierConflictException;
import ao.hospitalao.modules.patients.mapper.PatientMapper;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.security.tenant.TenantContext;
import jakarta.validation.Validation;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

  @Mock private PatientRepository patientRepository;
  @Mock private UserRepository userRepository;
  @Mock private PatientMapper patientMapper;

  @InjectMocks private PatientService patientService;

  @Test
  void createRejectsAnIdentifierAlreadyUsedInTheCurrentHospital() {
    UUID hospitalId = UUID.randomUUID();
    var request = new CreatePatientRequest();
    request.setNationalId("005123456LA042");

    try (MockedStatic<TenantContext> tenantContext = mockStatic(TenantContext.class)) {
      tenantContext.when(TenantContext::getCurrentHospital).thenReturn(hospitalId);
      when(patientRepository.existsByHospitalIdAndNationalId(hospitalId, "005123456LA042"))
          .thenReturn(true);

      assertThatThrownBy(() -> patientService.create(request))
          .isInstanceOf(PatientIdentifierConflictException.class);

      verify(patientRepository, never()).save(any());
    }
  }

  @Test
  void updateChangesIdentifiersAndAllowsThePatientsOwnIdentifiers() {
    UUID hospitalId = UUID.randomUUID();
    UUID patientId = UUID.randomUUID();
    Patient patient =
        Patient.builder()
            .id(patientId)
            .nationalId("OLD123456LA001")
            .healthCardNumber("OLD-CARD")
            .build();
    var request = new CreatePatientRequest();
    request.setNationalId(" 005123456la042 ");
    request.setHealthCardNumber(" CARD-001 ");
    var response = PatientResponse.builder().id(patientId).build();

    try (MockedStatic<TenantContext> tenantContext = mockStatic(TenantContext.class)) {
      tenantContext.when(TenantContext::getCurrentHospital).thenReturn(hospitalId);
      when(patientRepository.findByHospitalIdAndId(hospitalId, patientId))
          .thenReturn(Optional.of(patient));
      when(patientRepository.existsByHospitalIdAndNationalIdAndIdNot(
              hospitalId, "005123456LA042", patientId))
          .thenReturn(false);
      when(patientRepository.existsByHospitalIdAndHealthCardNumberAndIdNot(
              hospitalId, "CARD-001", patientId))
          .thenReturn(false);
      when(patientRepository.save(patient)).thenReturn(patient);
      when(patientMapper.toResponse(patient)).thenReturn(response);

      PatientResponse updated = patientService.update(patientId, request);

      assertThat(updated.getId()).isEqualTo(patientId);
      assertThat(patient.getNationalId()).isEqualTo("005123456LA042");
      assertThat(patient.getHealthCardNumber()).isEqualTo("CARD-001");
      verify(patientRepository).save(patient);
    }
  }

  @Test
  void possibleDuplicateCheckIsHospitalScopedAndCapped() {
    UUID hospitalId = UUID.randomUUID();
    LocalDate birthDate = LocalDate.of(1990, 4, 12);
    var candidate =
        new PatientDuplicateCandidateResponse(UUID.randomUUID(), "Ana Silva", birthDate);
    var request = new CheckPatientDuplicatesRequest(" Ana Silva ", birthDate, " +244 923 000 000 ");

    try (MockedStatic<TenantContext> tenantContext = mockStatic(TenantContext.class)) {
      tenantContext.when(TenantContext::getCurrentHospital).thenReturn(hospitalId);
      when(patientRepository.findPossibleDuplicates(
              org.mockito.ArgumentMatchers.eq(hospitalId),
              org.mockito.ArgumentMatchers.eq("Ana Silva"),
              org.mockito.ArgumentMatchers.eq(birthDate),
              org.mockito.ArgumentMatchers.eq("+244 923 000 000"),
              any(Pageable.class)))
          .thenReturn(List.of(candidate));

      assertThat(patientService.findPossibleDuplicates(request)).containsExactly(candidate);

      ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
      verify(patientRepository)
          .findPossibleDuplicates(
              org.mockito.ArgumentMatchers.eq(hospitalId),
              org.mockito.ArgumentMatchers.eq("Ana Silva"),
              org.mockito.ArgumentMatchers.eq(birthDate),
              org.mockito.ArgumentMatchers.eq("+244 923 000 000"),
              pageable.capture());
      assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
    }
  }

  @Test
  void requestAcceptsBiAndNifFormatsAndRejectsMalformedIdentifiers() {
    var request = new CreatePatientRequest();
    try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
      var validator = validatorFactory.getValidator();

      request.setNationalId("005123456LA042");
      assertThat(validator.validateProperty(request, "nationalId")).isEmpty();

      request.setNationalId("1234567890");
      assertThat(validator.validateProperty(request, "nationalId")).isEmpty();

      request.setNationalId("1234");
      assertThat(validator.validateProperty(request, "nationalId")).isNotEmpty();

      request.setNationalId("");
      assertThat(validator.validateProperty(request, "nationalId")).isEmpty();
    }
  }
}
