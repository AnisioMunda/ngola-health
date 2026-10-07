package ao.hospitalao.modules.inpatient.service;

import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.inpatient.dto.InpatientDtos.*;
import ao.hospitalao.modules.inpatient.entity.*;
import ao.hospitalao.modules.inpatient.entity.Admission.AdmissionStatus;
import ao.hospitalao.modules.inpatient.entity.Bed.BedStatus;
import ao.hospitalao.modules.inpatient.repository.AdmissionRepository;
import ao.hospitalao.modules.inpatient.repository.BedRepository;
import ao.hospitalao.modules.inpatient.repository.WardRepository;
import ao.hospitalao.security.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class InpatientService {

  private final WardRepository wardRepository;
  private final BedRepository bedRepository;
  private final AdmissionRepository admissionRepository;
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

    List<BedResponse> bedResponses = toBedResponsesWithAdmission(beds, wardId);

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
    String code = req.getCode().trim().toUpperCase(java.util.Locale.ROOT);

    if (wardRepository.existsByHospitalIdAndCode(hospitalId, code)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Código de enfermaria já existe: " + code);
    }

    Ward ward =
        Ward.builder()
            .hospital(hospitalRepository.getReferenceById(hospitalId))
            .name(req.getName().trim())
            .code(code)
            .type(req.getType() != null ? req.getType() : Ward.WardType.GENERAL)
            .floor(req.getFloor() != null ? req.getFloor().trim() : null)
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
    List<Bed> beds = bedRepository.findByWardIdAndActiveTrueOrderByBedNumber(wardId);
    return toBedResponsesWithAdmission(beds, wardId);
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
            .bedNumber(req.getBedNumber().trim())
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
    if (req.getStatus() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O estado da cama é obrigatório.");
    }
    if (req.getStatus() == BedStatus.OCCUPIED) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "O estado OCCUPIED só pode ser definido por um internamento.");
    }

    Bed bed =
        bedRepository
            .findByIdForUpdate(bedId)
            .orElseThrow(() -> new EntityNotFoundException("Cama não encontrada"));

    if (admissionRepository.isBedOccupied(bedId)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Não é possível alterar o estado de uma cama ocupada. Dê alta ao paciente primeiro.");
    }

    bed.setStatus(req.getStatus());
    if (req.getNotes() != null) bed.setNotes(req.getNotes());
    return toBedResponse(bedRepository.save(bed));
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
        .beds(
            w.getBeds().stream()
                .filter(Bed::isActive)
                .map(this::toBedResponse)
                .collect(Collectors.toList()))
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

  private List<BedResponse> toBedResponsesWithAdmission(List<Bed> beds, UUID wardId) {
    Map<UUID, Admission> admissionsByBed = new HashMap<>();
    if (beds.stream().anyMatch(bed -> bed.getStatus() == BedStatus.OCCUPIED)) {
      admissionRepository
          .findByWardIdAndStatus(wardId, AdmissionStatus.ACTIVE)
          .forEach(admission -> admissionsByBed.putIfAbsent(admission.getBed().getId(), admission));
    }
    return beds.stream()
        .map(
            bed -> {
              BedResponse response = toBedResponse(bed);
              Admission admission = admissionsByBed.get(bed.getId());
              if (bed.getStatus() == BedStatus.OCCUPIED && admission != null) {
                response.setPatientName(admission.getPatient().getFullName());
                response.setAdmissionId(admission.getId());
                response.setAdmissionDate(admission.getAdmissionDate());
              }
              return response;
            })
        .collect(Collectors.toList());
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
}
