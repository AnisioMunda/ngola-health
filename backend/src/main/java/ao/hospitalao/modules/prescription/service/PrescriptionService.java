package ao.hospitalao.modules.prescription.service;

import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.episodes.repository.EpisodeRepository;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.inpatient.repository.AdmissionRepository;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.modules.pharmacy.entity.Medication;
import ao.hospitalao.modules.pharmacy.entity.StockBatch;
import ao.hospitalao.modules.pharmacy.repository.MedicationRepository;
import ao.hospitalao.modules.pharmacy.repository.StockBatchRepository;
import ao.hospitalao.modules.prescription.dto.PrescriptionDtos.*;
import ao.hospitalao.modules.prescription.entity.*;
import ao.hospitalao.modules.prescription.entity.Prescription.PrescriptionStatus;
import ao.hospitalao.modules.prescription.entity.PrescriptionItem.ItemStatus;
import ao.hospitalao.modules.prescription.repository.DispensationRepository;
import ao.hospitalao.modules.prescription.repository.PrescriptionItemRepository;
import ao.hospitalao.modules.prescription.repository.PrescriptionRepository;
import ao.hospitalao.security.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionService {

  private static final ZoneId ANGOLA_ZONE = ZoneId.of("Africa/Luanda");

  private final PrescriptionRepository prescriptionRepository;
  private final PrescriptionItemRepository itemRepository;
  private final DispensationRepository dispensationRepository;
  private final MedicationRepository medicationRepository;
  private final StockBatchRepository stockBatchRepository;
  private final PatientRepository patientRepository;
  private final UserRepository userRepository;
  private final HospitalRepository hospitalRepository;
  private final EpisodeRepository episodeRepository;
  private final AdmissionRepository admissionRepository;

  // ------------------------------------------------
  // Criar prescrição
  // ------------------------------------------------

  @Transactional
  public PrescriptionResponse create(CreatePrescriptionRequest req) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    if (hospitalId == null || TenantContext.hasPlatformAccess()) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "As prescrições exigem um hospital activo.");
    }

    if (req.getItems() == null || req.getItems().isEmpty()) {
      throw new IllegalArgumentException("A prescrição deve ter pelo menos um medicamento.");
    }
    if (req.getEpisodeId() != null && req.getAdmissionId() != null) {
      throw new IllegalArgumentException(
          "A prescrição deve estar associada a um episódio ou a um internamento, não a ambos.");
    }

    LocalDate today = today();
    int validityDays = req.getValidityDays() != null ? req.getValidityDays() : 30;
    if (validityDays < 1 || validityDays > 365) {
      throw new IllegalArgumentException("A validade da prescrição deve estar entre 1 e 365 dias.");
    }

    var patient =
        patientRepository
            .findByHospitalIdAndId(hospitalId, req.getPatientId())
            .orElseThrow(() -> new EntityNotFoundException("Paciente não encontrado"));
    var medications = new HashMap<UUID, Medication>();
    Map<UUID, Long> requestedByMedication = new HashMap<>();
    for (var itemReq : req.getItems()) {
      var medication =
          medicationRepository
              .findByHospitalIdAndId(hospitalId, itemReq.getMedicationId())
              .orElseThrow(
                  () ->
                      new EntityNotFoundException(
                          "Medicamento não encontrado: " + itemReq.getMedicationId()));
      if (!medication.isActive()) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Não é possível prescrever o medicamento inactivo " + medication.getName() + ".");
      }
      medications.putIfAbsent(medication.getId(), medication);
      requestedByMedication.merge(
          medication.getId(), (long) itemReq.getQuantityPrescribed(), Long::sum);
    }

    for (var requested : requestedByMedication.entrySet()) {
      int available = stockBatchRepository.getTotalAvailableQuantity(requested.getKey(), today);
      if (available < requested.getValue()) {
        var medication = medications.get(requested.getKey());
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Stock insuficiente para "
                + medication.getName()
                + ". Disponível: "
                + available
                + ", necessário: "
                + requested.getValue());
      }
    }

    long seq = prescriptionRepository.nextPrescriptionNumber();
    String number = "RX-" + today.getYear() + "-" + String.format("%05d", seq);

    var doctor = getCurrentUser();

    Prescription prescription =
        Prescription.builder()
            .hospital(hospitalRepository.getReferenceById(hospitalId))
            .patient(patient)
            .doctor(doctor)
            .prescriptionNumber(number)
            .prescriptionDate(today)
            .expiryDate(today.plusDays(validityDays))
            .diagnosis(req.getDiagnosis())
            .notes(req.getNotes())
            .build();

    if (req.getEpisodeId() != null) {
      var episode =
          episodeRepository
              .findById(req.getEpisodeId())
              .orElseThrow(() -> new EntityNotFoundException("Episódio não encontrado"));
      if (!episode.getPatient().getId().equals(patient.getId())) {
        throw new IllegalArgumentException("O episódio não pertence ao paciente indicado.");
      }
      prescription.setEpisode(episode);
    }
    if (req.getAdmissionId() != null) {
      var admission =
          admissionRepository
              .findByIdWithRelations(req.getAdmissionId())
              .orElseThrow(() -> new EntityNotFoundException("Internamento não encontrado"));
      if (!admission.getPatient().getId().equals(patient.getId())) {
        throw new IllegalArgumentException("O internamento não pertence ao paciente indicado.");
      }
      prescription.setAdmission(admission);
    }

    for (var itemReq : req.getItems()) {
      PrescriptionItem item =
          PrescriptionItem.builder()
              .prescription(prescription)
              .medication(medications.get(itemReq.getMedicationId()))
              .quantityPrescribed(itemReq.getQuantityPrescribed())
              .dosage(itemReq.getDosage())
              .frequencyHours(itemReq.getFrequencyHours())
              .durationDays(itemReq.getDurationDays())
              .route(itemReq.getRoute())
              .instructions(itemReq.getInstructions())
              .build();

      prescription.getItems().add(item);
    }

    Prescription saved = prescriptionRepository.save(prescription);
    log.info("Prescription {} created for patient {}", number, req.getPatientId());

    return toResponse(saved);
  }

  // ------------------------------------------------
  // Queries
  // ------------------------------------------------

  @Transactional(readOnly = true)
  public PrescriptionResponse findById(UUID id) {
    return prescriptionRepository
        .findByIdWithRelations(id)
        .map(this::toResponseWithDispensations)
        .orElseThrow(() -> new EntityNotFoundException("Prescrição não encontrada"));
  }

  @Transactional(readOnly = true)
  public List<PrescriptionResponse> findByEpisode(UUID episodeId) {
    return prescriptionRepository.findByEpisodeIdOrderByCreatedAtDesc(episodeId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<PrescriptionResponse> findByAdmission(UUID admissionId) {
    return prescriptionRepository.findByAdmissionIdOrderByCreatedAtDesc(admissionId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public Page<PrescriptionResponse> findByPatient(UUID patientId, Pageable pageable) {
    return prescriptionRepository
        .findByPatientIdOrderByPrescriptionDateDesc(patientId, pageable)
        .map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public Page<PrescriptionResponse> findAll(LocalDate from, LocalDate to, Pageable pageable) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    LocalDate f = from != null ? from : today().minusMonths(1);
    LocalDate t = to != null ? to : today();
    return prescriptionRepository
        .findByHospitalAndPeriod(hospitalId, f, t, pageable)
        .map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public PrescriptionStatsDto getStats() {
    UUID hospitalId = TenantContext.getCurrentHospital();
    LocalDate today = today();

    long totalActive =
        prescriptionRepository
            .findByHospitalAndPeriod(
                hospitalId, today.minusMonths(1), today, PageRequest.of(0, 1000))
            .stream()
            .filter(
                p ->
                    p.getStatus() == PrescriptionStatus.ACTIVE
                        || p.getStatus() == PrescriptionStatus.PARTIALLY_DISPENSED)
            .count();

    long totalToday =
        prescriptionRepository
            .findByHospitalAndPeriod(hospitalId, today, today, PageRequest.of(0, 1000))
            .getTotalElements();

    long pendingDispense =
        prescriptionRepository
            .findByHospitalAndPeriod(
                hospitalId, today.minusMonths(1), today, PageRequest.of(0, 1000))
            .stream()
            .filter(
                p ->
                    p.getStatus() == PrescriptionStatus.ACTIVE
                        || p.getStatus() == PrescriptionStatus.PARTIALLY_DISPENSED)
            .count();

    long expiringSoon =
        prescriptionRepository.findExpiringBefore(hospitalId, today.plusDays(3)).size();

    return PrescriptionStatsDto.builder()
        .totalActive(totalActive)
        .totalToday(totalToday)
        .pendingDispense(pendingDispense)
        .expiringSoon(expiringSoon)
        .build();
  }

  // ------------------------------------------------
  // Dispensar medicamento (farmácia)
  // ------------------------------------------------

  @Transactional
  public PrescriptionResponse dispense(UUID prescriptionId, DispenseItemRequest req) {
    Prescription prescription = getOrThrow(prescriptionId);

    // Validações
    if (prescription.getStatus() == PrescriptionStatus.CANCELLED) {
      throw new IllegalStateException("Esta prescrição foi cancelada.");
    }
    if (prescription.getStatus() == PrescriptionStatus.DISPENSED) {
      throw new IllegalStateException("Esta prescrição já foi totalmente dispensada.");
    }
    if (prescription.isExpired()) {
      prescription.setStatus(PrescriptionStatus.EXPIRED);
      prescriptionRepository.save(prescription);
      throw new IllegalStateException(
          "Esta prescrição expirou em " + prescription.getExpiryDate() + ".");
    }

    PrescriptionItem item =
        itemRepository
            .findById(req.getPrescriptionItemId())
            .orElseThrow(() -> new EntityNotFoundException("Item de prescrição não encontrado"));

    if (item.getRemainingQuantity() < req.getQuantityToDispense()) {
      throw new IllegalStateException(
          "Quantidade solicitada ("
              + req.getQuantityToDispense()
              + ") excede a quantidade pendente ("
              + item.getRemainingQuantity()
              + ").");
    }

    // Seleccionar lote FEFO
    var batches =
        stockBatchRepository.findAvailableBatchesFefo(item.getMedication().getId(), today());

    if (batches.isEmpty()) {
      throw new IllegalStateException(
          "Sem stock disponível para " + item.getMedication().getName());
    }
    long available = batches.stream().mapToLong(StockBatch::getQuantityAvailable).sum();
    if (available < req.getQuantityToDispense()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Stock insuficiente. Disponível: "
              + available
              + ", solicitado: "
              + req.getQuantityToDispense());
    }

    int remaining = req.getQuantityToDispense();
    var dispensedBy = getCurrentUser();

    for (var batch : batches) {
      if (remaining <= 0) break;

      int fromBatch = Math.min(remaining, batch.getQuantityAvailable());
      batch.setQuantityAvailable(batch.getQuantityAvailable() - fromBatch);
      stockBatchRepository.save(batch);

      Dispensation dispensation =
          Dispensation.builder()
              .prescriptionItem(item)
              .medication(item.getMedication())
              .stockBatch(batch)
              .dispensedBy(dispensedBy)
              .quantityDispensed(fromBatch)
              .notes(req.getNotes())
              .build();

      dispensationRepository.save(dispensation);
      remaining -= fromBatch;
    }

    // Actualizar item
    item.setQuantityDispensed(item.getQuantityDispensed() + req.getQuantityToDispense());
    item.setStatus(item.isFullyDispensed() ? ItemStatus.DISPENSED : ItemStatus.PARTIAL);
    itemRepository.save(item);

    // Actualizar estado da prescrição
    updatePrescriptionStatus(prescription);

    log.info(
        "Dispensed {} units of {} for prescription {}",
        req.getQuantityToDispense(),
        item.getMedication().getName(),
        prescription.getPrescriptionNumber());

    return toResponseWithDispensations(getOrThrow(prescriptionId));
  }

  // ------------------------------------------------
  // Cancelar prescrição
  // ------------------------------------------------

  @Transactional
  public PrescriptionResponse cancel(UUID id, CancelPrescriptionRequest req) {
    Prescription prescription = getOrThrow(id);

    if (prescription.getStatus() == PrescriptionStatus.DISPENSED) {
      throw new IllegalStateException("Não é possível cancelar uma prescrição já dispensada.");
    }

    prescription.setStatus(PrescriptionStatus.CANCELLED);
    prescription.setCancelledReason(req.getReason());
    prescription.setCancelledBy(getCurrentUser());

    // Cancelar itens pendentes
    prescription.getItems().stream()
        .filter(i -> i.getStatus() == ItemStatus.PENDING)
        .forEach(
            i -> {
              i.setStatus(ItemStatus.CANCELLED);
              itemRepository.save(i);
            });

    return toResponse(prescriptionRepository.save(prescription));
  }

  // ------------------------------------------------
  // Job: marcar prescrições expiradas
  // ------------------------------------------------

  @Scheduled(cron = "0 0 1 * * *", zone = "Africa/Luanda") // todos os dias às 01h00
  @Transactional
  public void markExpiredPrescriptions() {
    log.info("Checking expired prescriptions...");
    hospitalRepository
        .findAll()
        .forEach(
            hospital -> {
              var expiring =
                  prescriptionRepository.findExpiringBefore(hospital.getId(), today().minusDays(1));
              expiring.stream()
                  .filter(p -> p.getStatus() == PrescriptionStatus.ACTIVE)
                  .forEach(
                      p -> {
                        p.setStatus(PrescriptionStatus.EXPIRED);
                        prescriptionRepository.save(p);
                      });
            });
  }

  // ------------------------------------------------
  // Helpers
  // ------------------------------------------------

  private void updatePrescriptionStatus(Prescription p) {
    boolean allDispensed =
        p.getItems().stream()
            .allMatch(
                i ->
                    i.getStatus() == ItemStatus.DISPENSED || i.getStatus() == ItemStatus.CANCELLED);
    boolean anyDispensed =
        p.getItems().stream()
            .anyMatch(
                i -> i.getStatus() == ItemStatus.DISPENSED || i.getStatus() == ItemStatus.PARTIAL);

    if (allDispensed) {
      p.setStatus(PrescriptionStatus.DISPENSED);
    } else if (anyDispensed) {
      p.setStatus(PrescriptionStatus.PARTIALLY_DISPENSED);
    }
    prescriptionRepository.save(p);
  }

  private Prescription getOrThrow(UUID id) {
    return prescriptionRepository
        .findByIdWithRelations(id)
        .orElseThrow(() -> new EntityNotFoundException("Prescrição não encontrada: " + id));
  }

  private LocalDate today() {
    return LocalDate.now(ANGOLA_ZONE);
  }

  private ao.hospitalao.modules.auth.entity.User getCurrentUser() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return userRepository.findByUsername(username).orElseThrow();
  }

  // ------------------------------------------------
  // Mappers
  // ------------------------------------------------

  private PrescriptionResponse toResponse(Prescription p) {
    List<PrescriptionItemResponse> items =
        p.getItems() != null ? p.getItems().stream().map(this::toItemResponse).toList() : List.of();

    return PrescriptionResponse.builder()
        .id(p.getId())
        .prescriptionNumber(p.getPrescriptionNumber())
        .patientId(p.getPatient().getId())
        .patientName(p.getPatient().getFullName())
        .doctorId(p.getDoctor().getId())
        .doctorName(p.getDoctor().getFullName())
        .episodeId(p.getEpisode() != null ? p.getEpisode().getId() : null)
        .admissionId(p.getAdmission() != null ? p.getAdmission().getId() : null)
        .status(p.getStatus())
        .statusLabel(statusLabel(p.getStatus()))
        .prescriptionDate(p.getPrescriptionDate())
        .expiryDate(p.getExpiryDate())
        .expired(p.isExpired())
        .diagnosis(p.getDiagnosis())
        .notes(p.getNotes())
        .cancelledReason(p.getCancelledReason())
        .items(items)
        .dispensations(List.of())
        .createdAt(p.getCreatedAt())
        .build();
  }

  private PrescriptionResponse toResponseWithDispensations(Prescription p) {
    var response = toResponse(p);
    var dispensations =
        dispensationRepository.findByPrescriptionId(p.getId()).stream()
            .map(this::toDispensationResponse)
            .toList();
    response.setDispensations(dispensations);
    return response;
  }

  private PrescriptionItemResponse toItemResponse(PrescriptionItem i) {
    int available =
        stockBatchRepository.getTotalAvailableQuantity(i.getMedication().getId(), today());

    return PrescriptionItemResponse.builder()
        .id(i.getId())
        .medicationId(i.getMedication().getId())
        .medicationName(i.getMedication().getName())
        .medicationUnit(i.getMedication().getUnit())
        .quantityPrescribed(i.getQuantityPrescribed())
        .quantityDispensed(i.getQuantityDispensed())
        .remainingQuantity(i.getRemainingQuantity())
        .dosage(i.getDosage())
        .frequencyHours(i.getFrequencyHours())
        .durationDays(i.getDurationDays())
        .route(i.getRoute())
        .instructions(i.getInstructions())
        .status(i.getStatus())
        .statusLabel(itemStatusLabel(i.getStatus()))
        .stockAvailable(available)
        .build();
  }

  private DispensationResponse toDispensationResponse(Dispensation d) {
    return DispensationResponse.builder()
        .id(d.getId())
        .prescriptionItemId(d.getPrescriptionItem().getId())
        .medicationName(d.getMedication().getName())
        .batchNumber(d.getStockBatch().getBatchNumber())
        .dispensedByName(d.getDispensedBy().getFullName())
        .quantityDispensed(d.getQuantityDispensed())
        .dispensedAt(d.getDispensedAt())
        .notes(d.getNotes())
        .build();
  }

  private String statusLabel(PrescriptionStatus s) {
    return switch (s) {
      case ACTIVE -> "Activa";
      case PARTIALLY_DISPENSED -> "Parcialmente Dispensada";
      case DISPENSED -> "Dispensada";
      case CANCELLED -> "Cancelada";
      case EXPIRED -> "Expirada";
    };
  }

  private String itemStatusLabel(ItemStatus s) {
    return switch (s) {
      case PENDING -> "Pendente";
      case DISPENSED -> "Dispensado";
      case PARTIAL -> "Parcial";
      case CANCELLED -> "Cancelado";
    };
  }
}
