package ao.hospitalao.modules.patients.service;

import ao.hospitalao.modules.auth.entity.User;
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
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientService {

  private final PatientRepository patientRepository;
  private final UserRepository userRepository;
  private final PatientMapper patientMapper;

  @Transactional(readOnly = true)
  public Page<PatientResponse> findAll(String search, Pageable pageable) {
    UUID hospitalId = requireHospitalScope();
    if (search != null && !search.isBlank()) {
      return patientRepository
          .search(hospitalId, search.trim(), pageable)
          .map(patientMapper::toResponse);
    }
    return patientRepository
        .findByHospitalIdAndActiveTrue(hospitalId, pageable)
        .map(patientMapper::toResponse);
  }

  @Transactional(readOnly = true)
  public PatientResponse findById(UUID id) {
    UUID hospitalId = requireHospitalScope();
    return patientRepository
        .findByHospitalIdAndId(hospitalId, id)
        .map(patientMapper::toResponse)
        .orElseThrow(() -> new EntityNotFoundException("Patient not found: " + id));
  }

  @Transactional(readOnly = true)
  public List<PatientDuplicateCandidateResponse> findPossibleDuplicates(
      CheckPatientDuplicatesRequest request) {
    UUID hospitalId = requireHospitalScope();
    return patientRepository.findPossibleDuplicates(
        hospitalId,
        request.fullName().trim(),
        request.birthDate(),
        normalizeOptional(request.phone()),
        PageRequest.of(0, 10));
  }

  @Transactional
  public PatientResponse create(CreatePatientRequest request) {
    UUID hospitalId = requireHospitalScope();
    validateUniqueIdentifiers(hospitalId, null, request);

    Patient patient =
        Patient.builder()
            .fullName(request.getFullName())
            .birthDate(request.getBirthDate())
            .gender(request.getGender())
            .nationalId(normalizeNationalId(request.getNationalId()))
            .healthCardNumber(normalizeOptional(request.getHealthCardNumber()))
            .phone(request.getPhone())
            .email(request.getEmail())
            .address(request.getAddress())
            .province(request.getProvince())
            .municipality(request.getMunicipality())
            .emergencyContactName(request.getEmergencyContactName())
            .emergencyContactPhone(request.getEmergencyContactPhone())
            .emergencyContactRelationship(request.getEmergencyContactRelationship())
            .bloodType(request.getBloodType())
            .allergies(request.getAllergies())
            .chronicConditions(request.getChronicConditions())
            .notes(request.getNotes())
            .createdBy(getCurrentUser())
            .build();

    Patient saved = patientRepository.save(patient);
    log.info("Patient created: {}", saved.getId());
    return patientMapper.toResponse(saved);
  }

  @Transactional
  public PatientResponse update(UUID id, CreatePatientRequest request) {
    UUID hospitalId = requireHospitalScope();
    Patient patient =
        patientRepository
            .findByHospitalIdAndId(hospitalId, id)
            .orElseThrow(() -> new EntityNotFoundException("Patient not found: " + id));

    validateUniqueIdentifiers(hospitalId, id, request);
    patient.setFullName(request.getFullName());
    patient.setBirthDate(request.getBirthDate());
    patient.setGender(request.getGender());
    patient.setNationalId(normalizeNationalId(request.getNationalId()));
    patient.setHealthCardNumber(normalizeOptional(request.getHealthCardNumber()));
    patient.setPhone(request.getPhone());
    patient.setEmail(request.getEmail());
    patient.setAddress(request.getAddress());
    patient.setProvince(request.getProvince());
    patient.setMunicipality(request.getMunicipality());
    patient.setEmergencyContactName(request.getEmergencyContactName());
    patient.setEmergencyContactPhone(request.getEmergencyContactPhone());
    patient.setEmergencyContactRelationship(request.getEmergencyContactRelationship());
    patient.setBloodType(request.getBloodType());
    patient.setAllergies(request.getAllergies());
    patient.setChronicConditions(request.getChronicConditions());
    patient.setNotes(request.getNotes());

    return patientMapper.toResponse(patientRepository.save(patient));
  }

  @Transactional
  public PatientResponse deactivate(UUID id) {
    UUID hospitalId = requireHospitalScope();
    Patient patient =
        patientRepository
            .findByHospitalIdAndId(hospitalId, id)
            .orElseThrow(() -> new EntityNotFoundException("Patient not found: " + id));
    patient.setActive(false);
    return patientMapper.toResponse(patientRepository.save(patient));
  }

  private User getCurrentUser() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return userRepository
        .findByUsername(username)
        .orElseThrow(
            () -> new EntityNotFoundException("Authenticated user not found: " + username));
  }

  private void validateUniqueIdentifiers(
      UUID hospitalId, UUID excludedPatientId, CreatePatientRequest request) {
    String nationalId = normalizeNationalId(request.getNationalId());
    if (nationalId != null
        && (excludedPatientId == null
            ? patientRepository.existsByHospitalIdAndNationalId(hospitalId, nationalId)
            : patientRepository.existsByHospitalIdAndNationalIdAndIdNot(
                hospitalId, nationalId, excludedPatientId))) {
      throw new PatientIdentifierConflictException();
    }

    String healthCardNumber = normalizeOptional(request.getHealthCardNumber());
    if (healthCardNumber != null
        && (excludedPatientId == null
            ? patientRepository.existsByHospitalIdAndHealthCardNumber(hospitalId, healthCardNumber)
            : patientRepository.existsByHospitalIdAndHealthCardNumberAndIdNot(
                hospitalId, healthCardNumber, excludedPatientId))) {
      throw new PatientIdentifierConflictException();
    }
  }

  private UUID requireHospitalScope() {
    UUID hospitalId = TenantContext.getCurrentHospital();
    if (hospitalId == null || TenantContext.hasPlatformAccess()) {
      throw new AccessDeniedException("Patient access requires a hospital scope.");
    }
    return hospitalId;
  }

  private static String normalizeNationalId(String nationalId) {
    String normalized = normalizeOptional(nationalId);
    return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
  }

  private static String normalizeOptional(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
