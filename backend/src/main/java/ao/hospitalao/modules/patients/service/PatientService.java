package ao.hospitalao.modules.patients.service;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.patients.dto.CreatePatientRequest;
import ao.hospitalao.modules.patients.dto.PatientResponse;
import ao.hospitalao.modules.patients.entity.Patient;
import ao.hospitalao.modules.patients.mapper.PatientMapper;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    if (search != null && !search.isBlank()) {
      return patientRepository.search(search.trim(), pageable).map(patientMapper::toResponse);
    }
    return patientRepository.findByActiveTrue(pageable).map(patientMapper::toResponse);
  }

  @Transactional(readOnly = true)
  public PatientResponse findById(UUID id) {
    return patientRepository
        .findById(id)
        .map(patientMapper::toResponse)
        .orElseThrow(() -> new EntityNotFoundException("Patient not found: " + id));
  }

  @Transactional
  public PatientResponse create(CreatePatientRequest request) {
    // Validate unique fields
    if (request.getNationalId() != null
        && patientRepository.existsByNationalId(request.getNationalId())) {
      throw new IllegalArgumentException(
          "National ID already registered: " + request.getNationalId());
    }
    if (request.getHealthCardNumber() != null
        && patientRepository.existsByHealthCardNumber(request.getHealthCardNumber())) {
      throw new IllegalArgumentException(
          "Health card already registered: " + request.getHealthCardNumber());
    }

    Patient patient =
        Patient.builder()
            .fullName(request.getFullName())
            .birthDate(request.getBirthDate())
            .gender(request.getGender())
            .nationalId(request.getNationalId())
            .healthCardNumber(request.getHealthCardNumber())
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
    log.info("Patient created: {} ({})", saved.getFullName(), saved.getId());
    return patientMapper.toResponse(saved);
  }

  @Transactional
  public PatientResponse update(UUID id, CreatePatientRequest request) {
    Patient patient =
        patientRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Patient not found: " + id));

    patient.setFullName(request.getFullName());
    patient.setBirthDate(request.getBirthDate());
    patient.setGender(request.getGender());
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
    Patient patient =
        patientRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Patient not found: " + id));
    patient.setActive(false);
    return patientMapper.toResponse(patientRepository.save(patient));
  }

  private User getCurrentUser() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return userRepository.findByUsername(username).orElse(null);
  }
}
