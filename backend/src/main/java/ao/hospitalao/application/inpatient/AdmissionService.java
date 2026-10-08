package ao.hospitalao.application.inpatient;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.episodes.repository.EpisodeRepository;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.inpatient.dto.InpatientDtos.AdmissionResponse;
import ao.hospitalao.modules.inpatient.dto.InpatientDtos.CreateAdmissionRequest;
import ao.hospitalao.modules.inpatient.entity.Admission;
import ao.hospitalao.modules.inpatient.entity.Admission.AdmissionStatus;
import ao.hospitalao.modules.inpatient.entity.Bed;
import ao.hospitalao.modules.inpatient.entity.Bed.BedStatus;
import ao.hospitalao.modules.inpatient.repository.AdmissionRepository;
import ao.hospitalao.modules.inpatient.repository.BedRepository;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.security.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdmissionService {

  private final AdmissionRepository admissionRepository;
  private final BedRepository bedRepository;
  private final PatientRepository patientRepository;
  private final UserRepository userRepository;
  private final HospitalRepository hospitalRepository;
  private final EpisodeRepository episodeRepository;
  private final InpatientUserProvider userProvider;
  private final AdmissionResponseMapper responseMapper;

  @Transactional(readOnly = true)
  public Page<AdmissionResponse> findAll(AdmissionStatus status, UUID wardId, Pageable pageable) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    return admissionRepository
        .findWithFilters(hospitalId, status, wardId, pageable)
        .map(responseMapper::toResponse);
  }

  @Transactional(readOnly = true)
  public AdmissionResponse findById(UUID id) {
    return admissionRepository
        .findByIdWithRelations(id)
        .map(responseMapper::toResponse)
        .orElseThrow(() -> new EntityNotFoundException("Internamento não encontrado"));
  }

  @Transactional(readOnly = true)
  public List<AdmissionResponse> findActiveByHospital() {
    UUID hospitalId = TenantContext.getCurrentHospital();
    return admissionRepository
        .findByHospitalIdAndStatusOrderByAdmissionDateDesc(hospitalId, AdmissionStatus.ACTIVE)
        .stream()
        .map(responseMapper::toResponse)
        .collect(Collectors.toList());
  }

  @Transactional
  public AdmissionResponse admit(CreateAdmissionRequest request) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    if (hospitalId == null || TenantContext.hasPlatformAccess()) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "O internamento exige um hospital activo.");
    }

    Bed bed = lockBed(request.getBedId());
    if (!bed.isActive()
        || (bed.getStatus() != BedStatus.AVAILABLE && bed.getStatus() != BedStatus.RESERVED)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "A cama não está disponível para internamento.");
    }

    admissionRepository
        .findByPatientIdAndStatus(request.getPatientId(), AdmissionStatus.ACTIVE)
        .ifPresent(
            existing -> {
              throw new ResponseStatusException(
                  HttpStatus.CONFLICT,
                  "O paciente já tem um internamento activo na enfermaria "
                      + existing.getWard().getName()
                      + ".");
            });

    var patient =
        patientRepository
            .findById(request.getPatientId())
            .orElseThrow(() -> new EntityNotFoundException("Paciente não encontrado"));
    User doctor =
        userRepository
            .findById(request.getResponsibleDoctorId())
            .orElseThrow(() -> new EntityNotFoundException("Médico não encontrado"));

    Admission admission =
        Admission.builder()
            .hospital(hospitalRepository.getReferenceById(hospitalId))
            .patient(patient)
            .bed(bed)
            .ward(bed.getWard())
            .responsibleDoctor(doctor)
            .admissionDate(OffsetDateTime.now())
            .admissionReason(request.getAdmissionReason().trim())
            .expectedDischargeDate(request.getExpectedDischargeDate())
            .admittedBy(userProvider.getCurrentUser())
            .build();

    if (request.getEpisodeId() != null) {
      admission.setEpisode(episodeRepository.getReferenceById(request.getEpisodeId()));
    }

    bed.setStatus(BedStatus.OCCUPIED);
    bedRepository.save(bed);
    Admission saved = admissionRepository.save(admission);
    log.info(
        "Patient {} admitted to bed {} in ward {}",
        patient.getFullName(),
        bed.getBedNumber(),
        bed.getWard().getName());
    return responseMapper.toResponse(saved);
  }

  private Bed lockBed(UUID id) {
    return bedRepository
        .findByIdForUpdate(id)
        .orElseThrow(() -> new EntityNotFoundException("Cama não encontrada: " + id));
  }
}
