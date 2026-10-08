package ao.hospitalao.modules.triage.service;

import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.modules.triage.dto.TriageDtos.*;
import ao.hospitalao.modules.triage.entity.TriageRecord;
import ao.hospitalao.modules.triage.entity.TriageRecord.TriagePriority;
import ao.hospitalao.modules.triage.entity.TriageRecord.TriageStatus;
import ao.hospitalao.modules.triage.repository.TriageRepository;
import ao.hospitalao.security.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TriageService {

  private static final ZoneId ANGOLA_ZONE = ZoneId.of("Africa/Luanda");

  private final TriageRepository triageRepository;
  private final PatientRepository patientRepository;
  private final UserRepository userRepository;
  private final HospitalRepository hospitalRepository;

  // ------------------------------------------------
  // Stats
  // ------------------------------------------------

  @Transactional(readOnly = true)
  public TriageStatsDto getStats() {
    UUID hospitalId = TenantContext.getCurrentHospital();
    return TriageStatsDto.builder()
        .totalWaiting(triageRepository.countTodayByStatus(hospitalId, TriageStatus.WAITING))
        .totalToday(triageRepository.countToday(hospitalId))
        .inProgress(triageRepository.countTodayByStatus(hospitalId, TriageStatus.IN_PROGRESS))
        .completedToday(triageRepository.countTodayByStatus(hospitalId, TriageStatus.COMPLETED))
        .waitingRed(triageRepository.countWaitingByPriority(hospitalId, TriagePriority.RED))
        .waitingOrange(triageRepository.countWaitingByPriority(hospitalId, TriagePriority.ORANGE))
        .waitingYellow(triageRepository.countWaitingByPriority(hospitalId, TriagePriority.YELLOW))
        .waitingGreen(triageRepository.countWaitingByPriority(hospitalId, TriagePriority.GREEN))
        .waitingBlue(triageRepository.countWaitingByPriority(hospitalId, TriagePriority.BLUE))
        .avgWaitingMinutes(triageRepository.avgWaitingMinutesToday(hospitalId))
        .build();
  }

  // ------------------------------------------------
  // Fila de espera
  // ------------------------------------------------

  @Transactional(readOnly = true)
  public List<TriageResponse> getActiveQueue() {
    UUID hospitalId = TenantContext.getCurrentHospital();
    return triageRepository.findActiveQueue(hospitalId).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<TriageResponse> getWaitingQueue() {
    UUID hospitalId = TenantContext.getCurrentHospital();
    return triageRepository.findWaitingQueue(hospitalId).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<TriageResponse> getHistory(LocalDate date) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    LocalDate d = date != null ? date : LocalDate.now(ANGOLA_ZONE);

    OffsetDateTime startOfDay = d.atStartOfDay(ANGOLA_ZONE).toOffsetDateTime();
    OffsetDateTime endOfDay = startOfDay.plusDays(1);

    return triageRepository.findByDate(hospitalId, startOfDay, endOfDay).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public TriageResponse findById(UUID id) {
    return triageRepository
        .findById(id)
        .map(this::toResponse)
        .orElseThrow(() -> new EntityNotFoundException("Triagem não encontrada"));
  }

  // ------------------------------------------------
  // Criar triagem
  // ------------------------------------------------

  @Transactional
  public TriageResponse create(CreateTriageRequest req) {
    UUID hospitalId = TenantContext.getCurrentHospital();

    if (req.getPatientId() == null
        && (req.getPatientNameTemp() == null || req.getPatientNameTemp().isBlank())) {
      throw new IllegalArgumentException(
          "É necessário identificar o paciente ou indicar um nome temporário.");
    }

    long queueNum = triageRepository.nextQueueNumber();

    TriageRecord record =
        TriageRecord.builder()
            .hospital(hospitalRepository.getReferenceById(hospitalId))
            .queueNumber((int) queueNum)
            .priority(req.getPriority() != null ? req.getPriority() : TriagePriority.GREEN)
            .chiefComplaint(req.getChiefComplaint())
            .patientNameTemp(
                req.getPatientNameTemp() != null ? req.getPatientNameTemp().trim() : null)
            .patientAgeTemp(req.getPatientAgeTemp())
            .patientGenderTemp(req.getPatientGenderTemp())
            .bloodPressure(req.getBloodPressure())
            .heartRate(req.getHeartRate())
            .temperature(req.getTemperature())
            .oxygenSaturation(req.getOxygenSaturation())
            .respiratoryRate(req.getRespiratoryRate())
            .weightKg(req.getWeightKg())
            .painScale(req.getPainScale())
            .triageNotes(req.getTriageNotes())
            .triagedBy(getCurrentUser())
            .build();

    if (req.getPatientId() != null) {
      record.setPatient(patientRepository.getReferenceById(req.getPatientId()));
    }

    TriageRecord saved = triageRepository.save(record);
    log.info("Triage record created");
    return toResponse(saved);
  }

  // ------------------------------------------------
  // Actualizar prioridade
  // ------------------------------------------------

  @Transactional
  public TriageResponse updatePriority(UUID id, UpdatePriorityRequest req) {
    TriageRecord record = getOrThrow(id);
    if (record.getStatus() != TriageStatus.WAITING) {
      throw new IllegalStateException(
          "Só é possível alterar a prioridade enquanto o paciente espera.");
    }
    if (req.getPriority() == null) {
      throw new IllegalArgumentException("A prioridade é obrigatória.");
    }
    TriagePriority oldPriority = record.getPriority();
    record.setPriority(req.getPriority());
    if (req.getReason() != null) {
      String note =
          "[Prioridade alterada de "
              + oldPriority
              + " para "
              + req.getPriority()
              + ": "
              + req.getReason()
              + "]";
      record.setTriageNotes(
          record.getTriageNotes() != null ? record.getTriageNotes() + "\n" + note : note);
    }
    return toResponse(triageRepository.save(record));
  }

  // ------------------------------------------------
  // Chamar próximo
  // ------------------------------------------------

  @Transactional
  public TriageResponse callNext(UUID id) {
    TriageRecord record = getOrThrow(id);
    if (record.getStatus() != TriageStatus.WAITING) {
      throw new IllegalStateException("Este paciente não está em espera.");
    }
    record.setStatus(TriageStatus.IN_PROGRESS);
    record.setAttendedAt(OffsetDateTime.now());
    log.info("Triage record called");
    return toResponse(triageRepository.save(record));
  }

  // ------------------------------------------------
  // Completar / dar alta
  // ------------------------------------------------

  @Transactional
  public TriageResponse complete(UUID id) {
    TriageRecord record = getOrThrow(id);
    if (record.getStatus() != TriageStatus.IN_PROGRESS) {
      throw new IllegalStateException("Só é possível concluir uma triagem em atendimento.");
    }
    record.setStatus(TriageStatus.COMPLETED);
    record.setCompletedAt(OffsetDateTime.now());
    return toResponse(triageRepository.save(record));
  }

  // ------------------------------------------------
  // Marcar como saiu sem ser atendido
  // ------------------------------------------------

  @Transactional
  public TriageResponse markAsLeft(UUID id) {
    TriageRecord record = getOrThrow(id);
    if (record.getStatus() != TriageStatus.WAITING) {
      throw new IllegalStateException("Apenas pacientes em espera podem sair.");
    }
    record.setStatus(TriageStatus.LEFT);
    record.setCompletedAt(OffsetDateTime.now());
    return toResponse(triageRepository.save(record));
  }

  // ------------------------------------------------
  // Helpers
  // ------------------------------------------------

  private TriageRecord getOrThrow(UUID id) {
    return triageRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Triagem não encontrada: " + id));
  }

  private ao.hospitalao.modules.auth.entity.User getCurrentUser() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return userRepository.findByUsername(username).orElse(null);
  }

  private TriageResponse toResponse(TriageRecord t) {
    return TriageResponse.builder()
        .id(t.getId())
        .queueNumber(t.getQueueNumber())
        .patientId(t.getPatient() != null ? t.getPatient().getId() : null)
        .patientName(t.getDisplayName())
        .patientAge(t.getPatient() != null ? null : t.getPatientAgeTemp())
        .patientGender(t.getPatient() != null ? null : t.getPatientGenderTemp())
        .priority(t.getPriority())
        .priorityLabel(priorityLabel(t.getPriority()))
        .priorityColor(priorityColor(t.getPriority()))
        .chiefComplaint(t.getChiefComplaint())
        .bloodPressure(t.getBloodPressure())
        .heartRate(t.getHeartRate())
        .temperature(t.getTemperature())
        .oxygenSaturation(t.getOxygenSaturation())
        .respiratoryRate(t.getRespiratoryRate())
        .weightKg(t.getWeightKg())
        .painScale(t.getPainScale())
        .triageNotes(t.getTriageNotes())
        .status(t.getStatus())
        .statusLabel(statusLabel(t.getStatus()))
        .triagedByName(t.getTriagedBy() != null ? t.getTriagedBy().getFullName() : null)
        .episodeId(t.getEpisode() != null ? t.getEpisode().getId() : null)
        .triagedAt(t.getTriagedAt())
        .attendedAt(t.getAttendedAt())
        .completedAt(t.getCompletedAt())
        .waitingMinutes(t.getWaitingMinutes())
        .overdue(t.isOverdue())
        .build();
  }

  private String priorityLabel(TriagePriority p) {
    return switch (p) {
      case RED -> "Imediato";
      case ORANGE -> "Muito Urgente";
      case YELLOW -> "Urgente";
      case GREEN -> "Pouco Urgente";
      case BLUE -> "Não Urgente";
    };
  }

  private String priorityColor(TriagePriority p) {
    return switch (p) {
      case RED -> "#dc2626";
      case ORANGE -> "#ea580c";
      case YELLOW -> "#ca8a04";
      case GREEN -> "#16a34a";
      case BLUE -> "#2563eb";
    };
  }

  private String statusLabel(TriageStatus s) {
    return switch (s) {
      case WAITING -> "Em Espera";
      case IN_PROGRESS -> "Em Atendimento";
      case COMPLETED -> "Concluído";
      case TRANSFERRED -> "Transferido";
      case LEFT -> "Saiu";
    };
  }
}
