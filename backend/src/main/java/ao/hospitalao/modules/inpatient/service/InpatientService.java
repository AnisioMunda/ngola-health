package ao.hospitalao.modules.inpatient.service;

import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.episodes.repository.EpisodeRepository;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.inpatient.dto.InpatientDtos.*;
import ao.hospitalao.modules.inpatient.entity.*;
import ao.hospitalao.modules.inpatient.entity.Admission.AdmissionStatus;
import ao.hospitalao.modules.inpatient.entity.Bed.BedStatus;
import ao.hospitalao.modules.inpatient.repository.AdmissionRepository;
import ao.hospitalao.modules.inpatient.repository.BedRepository;
import ao.hospitalao.modules.inpatient.repository.WardRepository;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InpatientService {

  private final WardRepository wardRepository;
  private final BedRepository bedRepository;
  private final AdmissionRepository admissionRepository;
  private final PatientRepository patientRepository;
  private final EpisodeRepository episodeRepository;
  private final UserRepository userRepository;
  private final HospitalRepository hospitalRepository;

  // ------------------------------------------------
  // Enfermarias
  // ------------------------------------------------

  @Transactional(readOnly = true)
  public List<WardResponse> findAllWards() {
    UUID hospitalId = TenantContext.getCurrentHospital();
    return wardRepository.findWithBedsAndDoctor(hospitalId).stream()
        .map(this::toWardResponse)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public WardMapResponse getWardMap(UUID wardId) {
    Ward ward =
        wardRepository
            .findById(wardId)
            .orElseThrow(() -> new EntityNotFoundException("Enfermaria não encontrada"));

    List<Bed> beds = bedRepository.findByWardIdAndActiveTrueOrderByBedNumber(wardId);

    long available = beds.stream().filter(b -> b.getStatus() == BedStatus.AVAILABLE).count();
    long occupied = beds.stream().filter(b -> b.getStatus() == BedStatus.OCCUPIED).count();
    long maintenance = beds.stream().filter(b -> b.getStatus() == BedStatus.MAINTENANCE).count();

    List<BedResponse> bedResponses =
        beds.stream().map(this::toBedResponseWithAdmission).collect(Collectors.toList());

    return WardMapResponse.builder()
        .wardId(ward.getId())
        .wardName(ward.getName())
        .wardType(ward.getType().name())
        .totalBeds(beds.size())
        .availableBeds((int) available)
        .occupiedBeds((int) occupied)
        .maintenanceBeds((int) maintenance)
        .beds(bedResponses)
        .build();
  }

  @Transactional
  public WardResponse createWard(CreateWardRequest req) {
    UUID hospitalId = TenantContext.getCurrentHospital();

    if (wardRepository.existsByHospitalIdAndCode(hospitalId, req.getCode())) {
      throw new IllegalArgumentException("Código de enfermaria já existe: " + req.getCode());
    }

    Ward ward =
        Ward.builder()
            .hospital(hospitalRepository.getReferenceById(hospitalId))
            .name(req.getName())
            .code(req.getCode().toUpperCase())
            .type(req.getType() != null ? req.getType() : Ward.WardType.GENERAL)
            .floor(req.getFloor())
            .notes(req.getNotes())
            .build();

    if (req.getResponsibleDoctorId() != null) {
      ward.setResponsibleDoctor(userRepository.getReferenceById(req.getResponsibleDoctorId()));
    }

    return toWardResponse(wardRepository.save(ward));
  }

  // ------------------------------------------------
  // Camas
  // ------------------------------------------------

  @Transactional(readOnly = true)
  public List<BedResponse> findBedsByWard(UUID wardId) {
    return bedRepository.findByWardIdAndActiveTrueOrderByBedNumber(wardId).stream()
        .map(this::toBedResponseWithAdmission)
        .collect(Collectors.toList());
  }

  @Transactional
  public BedResponse createBed(CreateBedRequest req) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    Ward ward =
        wardRepository
            .findById(req.getWardId())
            .orElseThrow(() -> new EntityNotFoundException("Enfermaria não encontrada"));

    Bed bed =
        Bed.builder()
            .ward(ward)
            .hospital(hospitalRepository.getReferenceById(hospitalId))
            .bedNumber(req.getBedNumber())
            .type(req.getType() != null ? req.getType() : Bed.BedType.STANDARD)
            .notes(req.getNotes())
            .build();

    // Actualizar totalBeds na enfermaria
    ward.setTotalBeds(ward.getTotalBeds() + 1);
    wardRepository.save(ward);

    return toBedResponse(bedRepository.save(bed));
  }

  @Transactional
  public BedResponse updateBedStatus(UUID bedId, UpdateBedStatusRequest req) {
    Bed bed =
        bedRepository
            .findById(bedId)
            .orElseThrow(() -> new EntityNotFoundException("Cama não encontrada"));

    // Não pode mudar status de cama ocupada directamente
    if (bed.getStatus() == BedStatus.OCCUPIED && req.getStatus() != BedStatus.OCCUPIED) {
      if (admissionRepository.isBedOccupied(bedId)) {
        throw new IllegalStateException(
            "Não é possível alterar o estado de uma cama ocupada. Dê alta ao paciente primeiro.");
      }
    }

    bed.setStatus(req.getStatus());
    if (req.getNotes() != null) bed.setNotes(req.getNotes());
    return toBedResponse(bedRepository.save(bed));
  }

  // ------------------------------------------------
  // Internamentos
  // ------------------------------------------------

  @Transactional(readOnly = true)
  public Page<AdmissionResponse> findAll(AdmissionStatus status, UUID wardId, Pageable pageable) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    return admissionRepository
        .findWithFilters(hospitalId, status, wardId, pageable)
        .map(this::toAdmissionResponse);
  }

  @Transactional(readOnly = true)
  public AdmissionResponse findById(UUID id) {
    return admissionRepository
        .findByIdWithRelations(id)
        .map(this::toAdmissionResponse)
        .orElseThrow(() -> new EntityNotFoundException("Internamento não encontrado"));
  }

  @Transactional(readOnly = true)
  public List<AdmissionResponse> findActiveByHospital() {
    UUID hospitalId = TenantContext.getCurrentHospital();
    return admissionRepository
        .findByHospitalIdAndStatusOrderByAdmissionDateDesc(hospitalId, AdmissionStatus.ACTIVE)
        .stream()
        .map(this::toAdmissionResponse)
        .collect(Collectors.toList());
  }

  @Transactional
  public AdmissionResponse admit(CreateAdmissionRequest req) {
    UUID hospitalId = TenantContext.getCurrentHospital();

    // Verificar se paciente já tem internamento activo
    admissionRepository
        .findByPatientIdAndStatus(req.getPatientId(), AdmissionStatus.ACTIVE)
        .ifPresent(
            a -> {
              throw new IllegalStateException(
                  "Paciente já tem um internamento activo na " + a.getWard().getName());
            });

    // Verificar se cama está disponível
    Bed bed =
        bedRepository
            .findById(req.getBedId())
            .orElseThrow(() -> new EntityNotFoundException("Cama não encontrada"));
    if (bed.getStatus() != BedStatus.AVAILABLE && bed.getStatus() != BedStatus.RESERVED) {
      throw new IllegalStateException(
          "Cama " + bed.getBedNumber() + " não está disponível. Estado: " + bed.getStatus());
    }

    var patient =
        patientRepository
            .findById(req.getPatientId())
            .orElseThrow(() -> new EntityNotFoundException("Paciente não encontrado"));
    var doctor =
        userRepository
            .findById(req.getResponsibleDoctorId())
            .orElseThrow(() -> new EntityNotFoundException("Médico não encontrado"));

    Admission admission =
        Admission.builder()
            .hospital(hospitalRepository.getReferenceById(hospitalId))
            .patient(patient)
            .bed(bed)
            .ward(bed.getWard())
            .responsibleDoctor(doctor)
            .admissionDate(OffsetDateTime.now())
            .admissionReason(req.getAdmissionReason())
            .expectedDischargeDate(req.getExpectedDischargeDate())
            .admittedBy(getCurrentUser())
            .build();

    if (req.getEpisodeId() != null) {
      admission.setEpisode(episodeRepository.getReferenceById(req.getEpisodeId()));
    }

    // Marcar cama como ocupada
    bed.setStatus(BedStatus.OCCUPIED);
    bedRepository.save(bed);

    Admission saved = admissionRepository.save(admission);
    log.info(
        "Patient {} admitted to bed {} in ward {}",
        patient.getFullName(),
        bed.getBedNumber(),
        bed.getWard().getName());

    return toAdmissionResponse(saved);
  }

  @Transactional
  public AdmissionResponse discharge(UUID id, DischargeRequest req) {
    Admission admission = getOrThrow(id);

    if (admission.getStatus() != AdmissionStatus.ACTIVE) {
      throw new IllegalStateException("Apenas internamentos ACTIVE podem receber alta.");
    }

    admission.setStatus(AdmissionStatus.DISCHARGED);
    admission.setDischargeDate(OffsetDateTime.now());
    admission.setDischargeNotes(req.getDischargeNotes());
    admission.setDischargeCondition(req.getDischargeCondition());
    admission.setDischargedBy(getCurrentUser());

    // Libertar cama
    Bed bed = admission.getBed();
    bed.setStatus(BedStatus.AVAILABLE);
    bedRepository.save(bed);

    log.info(
        "Patient {} discharged from bed {}",
        admission.getPatient().getFullName(),
        bed.getBedNumber());
    return toAdmissionResponse(admissionRepository.save(admission));
  }

  @Transactional
  public AdmissionResponse transfer(UUID id, TransferRequest req) {
    Admission admission = getOrThrow(id);

    if (admission.getStatus() != AdmissionStatus.ACTIVE) {
      throw new IllegalStateException("Apenas internamentos activos podem ser transferidos.");
    }

    Bed newBed =
        bedRepository
            .findById(req.getToBedId())
            .orElseThrow(() -> new EntityNotFoundException("Cama de destino não encontrada"));

    if (newBed.getStatus() != BedStatus.AVAILABLE && newBed.getStatus() != BedStatus.RESERVED) {
      throw new IllegalStateException("Cama de destino não está disponível.");
    }

    // Registar transferência
    BedTransfer transfer =
        BedTransfer.builder()
            .admission(admission)
            .fromBed(admission.getBed())
            .fromWard(admission.getWard())
            .toBed(newBed)
            .toWard(newBed.getWard())
            .reason(req.getReason())
            .transferredBy(getCurrentUser())
            .build();
    admission.getTransfers().add(transfer);

    // Libertar cama antiga
    Bed oldBed = admission.getBed();
    oldBed.setStatus(BedStatus.AVAILABLE);
    bedRepository.save(oldBed);

    // Actualizar internamento
    admission.setBed(newBed);
    admission.setWard(newBed.getWard());

    // Ocupar nova cama
    newBed.setStatus(BedStatus.OCCUPIED);
    bedRepository.save(newBed);

    log.info(
        "Patient {} transferred from bed {} to bed {}",
        admission.getPatient().getFullName(),
        oldBed.getBedNumber(),
        newBed.getBedNumber());
    return toAdmissionResponse(admissionRepository.save(admission));
  }

  // ------------------------------------------------
  // Helpers
  // ------------------------------------------------

  private Admission getOrThrow(UUID id) {
    return admissionRepository
        .findByIdWithRelations(id)
        .orElseThrow(() -> new EntityNotFoundException("Internamento não encontrado: " + id));
  }

  private ao.hospitalao.modules.auth.entity.User getCurrentUser() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return userRepository.findByUsername(username).orElse(null);
  }

  private WardResponse toWardResponse(Ward w) {
    long available =
        w.getBeds().stream()
            .filter(b -> b.getStatus() == BedStatus.AVAILABLE && b.isActive())
            .count();
    long occupied =
        w.getBeds().stream()
            .filter(b -> b.getStatus() == BedStatus.OCCUPIED && b.isActive())
            .count();

    return WardResponse.builder()
        .id(w.getId())
        .name(w.getName())
        .code(w.getCode())
        .type(w.getType())
        .typeLabel(wardTypeLabel(w.getType()))
        .floor(w.getFloor())
        .totalBeds(w.getTotalBeds())
        .availableBeds((int) available)
        .occupiedBeds((int) occupied)
        .responsibleDoctorName(
            w.getResponsibleDoctor() != null ? w.getResponsibleDoctor().getFullName() : null)
        .notes(w.getNotes())
        .active(w.isActive())
        .build();
  }

  private BedResponse toBedResponse(Bed b) {
    return BedResponse.builder()
        .id(b.getId())
        .bedNumber(b.getBedNumber())
        .status(b.getStatus())
        .statusLabel(bedStatusLabel(b.getStatus()))
        .type(b.getType())
        .wardName(b.getWard().getName())
        .wardId(b.getWard().getId())
        .notes(b.getNotes())
        .build();
  }

  private BedResponse toBedResponseWithAdmission(Bed b) {
    BedResponse resp = toBedResponse(b);
    if (b.getStatus() == BedStatus.OCCUPIED) {
      admissionRepository.findByPatientIdAndStatus(null, AdmissionStatus.ACTIVE);
      // Buscar internamento activo desta cama
      admissionRepository
          .findWithFilters(
              b.getHospital().getId(),
              AdmissionStatus.ACTIVE,
              b.getWard().getId(),
              org.springframework.data.domain.PageRequest.of(0, 100))
          .getContent()
          .stream()
          .filter(a -> a.getBed().getId().equals(b.getId()))
          .findFirst()
          .ifPresent(
              a -> {
                resp.setPatientName(a.getPatient().getFullName());
                resp.setAdmissionId(a.getId());
                resp.setAdmissionDate(a.getAdmissionDate());
              });
    }
    return resp;
  }

  private AdmissionResponse toAdmissionResponse(Admission a) {
    List<TransferResponse> transfers =
        a.getTransfers() != null
            ? a.getTransfers().stream()
                .map(
                    t ->
                        TransferResponse.builder()
                            .id(t.getId())
                            .fromBedNumber(t.getFromBed().getBedNumber())
                            .fromWardName(t.getFromWard().getName())
                            .toBedNumber(t.getToBed().getBedNumber())
                            .toWardName(t.getToWard().getName())
                            .reason(t.getReason())
                            .transferredByName(
                                t.getTransferredBy() != null
                                    ? t.getTransferredBy().getFullName()
                                    : null)
                            .transferredAt(t.getTransferredAt())
                            .build())
                .collect(Collectors.toList())
            : List.of();

    return AdmissionResponse.builder()
        .id(a.getId())
        .patientId(a.getPatient().getId())
        .patientName(a.getPatient().getFullName())
        .patientPhone(a.getPatient().getPhone())
        .bedId(a.getBed().getId())
        .bedNumber(a.getBed().getBedNumber())
        .wardId(a.getWard().getId())
        .wardName(a.getWard().getName())
        .wardType(a.getWard().getType().name())
        .doctorId(a.getResponsibleDoctor().getId())
        .doctorName(a.getResponsibleDoctor().getFullName())
        .status(a.getStatus())
        .statusLabel(admissionStatusLabel(a.getStatus()))
        .admissionDate(a.getAdmissionDate())
        .expectedDischargeDate(a.getExpectedDischargeDate())
        .dischargeDate(a.getDischargeDate())
        .admissionReason(a.getAdmissionReason())
        .diagnosis(a.getDiagnosis())
        .dischargeNotes(a.getDischargeNotes())
        .dischargeCondition(a.getDischargeCondition())
        .daysAdmitted(a.getDaysAdmitted())
        .admittedByName(a.getAdmittedBy() != null ? a.getAdmittedBy().getFullName() : null)
        .dischargedByName(a.getDischargedBy() != null ? a.getDischargedBy().getFullName() : null)
        .createdAt(a.getCreatedAt())
        .transfers(transfers)
        .build();
  }

  private String wardTypeLabel(Ward.WardType t) {
    return switch (t) {
      case GENERAL -> "Medicina Geral";
      case PEDIATRIC -> "Pediatria";
      case MATERNITY -> "Maternidade";
      case ICU -> "UCI";
      case SURGICAL -> "Cirurgia";
      case CARDIOLOGY -> "Cardiologia";
      case ONCOLOGY -> "Oncologia";
      case EMERGENCY -> "Urgência";
      case ISOLATION -> "Isolamento";
    };
  }

  private String bedStatusLabel(BedStatus s) {
    return switch (s) {
      case AVAILABLE -> "Disponível";
      case OCCUPIED -> "Ocupada";
      case MAINTENANCE -> "Manutenção";
      case RESERVED -> "Reservada";
    };
  }

  private String admissionStatusLabel(AdmissionStatus s) {
    return switch (s) {
      case ACTIVE -> "Internado";
      case DISCHARGED -> "Alta";
      case TRANSFERRED -> "Transferido";
      case DECEASED -> "Óbito";
    };
  }
}
