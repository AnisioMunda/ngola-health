package ao.hospitalao.modules.pharmacy.service;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.episodes.application.EpisodeApplicationService;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.modules.pharmacy.dto.PharmacyDtos.*;
import ao.hospitalao.modules.pharmacy.entity.Medication;
import ao.hospitalao.modules.pharmacy.entity.StockBatch;
import ao.hospitalao.modules.pharmacy.entity.StockMovement;
import ao.hospitalao.modules.pharmacy.entity.StockMovement.MovementType;
import ao.hospitalao.modules.pharmacy.repository.MedicationRepository;
import ao.hospitalao.modules.pharmacy.repository.StockBatchRepository;
import ao.hospitalao.modules.pharmacy.repository.StockMovementRepository;
import ao.hospitalao.security.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PharmacyService {

  private static final ZoneId ANGOLA_ZONE = ZoneId.of("Africa/Luanda");

  private final MedicationRepository medicationRepository;
  private final StockBatchRepository batchRepository;
  private final StockMovementRepository movementRepository;
  private final PatientRepository patientRepository;
  private final EpisodeApplicationService episodeApplicationService;
  private final UserRepository userRepository;
  private final HospitalRepository hospitalRepository;

  // ------------------------------------------------
  // Medications
  // ------------------------------------------------

  @Transactional(readOnly = true)
  public Page<MedicationResponse> findAllMedications(String search, Pageable pageable) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    Page<Medication> page =
        (search != null && !search.isBlank())
            ? medicationRepository.search(hospitalId, search.trim(), pageable)
            : medicationRepository.findByHospitalIdAndActiveTrue(hospitalId, pageable);
    return page.map(this::toMedicationResponse);
  }

  @Transactional(readOnly = true)
  public MedicationResponse findMedication(UUID medicationId) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    return toMedicationResponse(findMedicationOrThrow(hospitalId, medicationId));
  }

  @Transactional
  public MedicationResponse createMedication(CreateMedicationRequest req) {
    UUID hospitalId = TenantContext.getCurrentHospital();

    Medication med =
        Medication.builder()
            .hospital(hospitalRepository.getReferenceById(hospitalId))
            .name(req.getName())
            .genericName(req.getGenericName())
            .dosageForm(req.getDosageForm())
            .strength(req.getStrength())
            .unit(req.getUnit())
            .requiresPrescription(req.isRequiresPrescription())
            .minStockLevel(req.getMinStockLevel() != null ? req.getMinStockLevel() : 10)
            .build();

    Medication saved = medicationRepository.save(med);
    log.info("Medication created");
    return toMedicationResponse(saved);
  }

  // ------------------------------------------------
  // Stock Batches
  // ------------------------------------------------

  @Transactional(readOnly = true)
  public List<StockBatchResponse> findBatchesByMedication(UUID medicationId) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    findMedicationOrThrow(hospitalId, medicationId);
    return batchRepository.findByMedicationIdAndHospitalId(medicationId, hospitalId).stream()
        .map(this::toBatchResponse)
        .collect(Collectors.toList());
  }

  @Transactional
  public StockBatchResponse receiveStock(ReceiveStockRequest req) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    Medication med = findMedicationOrThrow(hospitalId, req.getMedicationId());
    if (!med.isActive()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Não é possível receber stock para um medicamento inactivo.");
    }
    if (req.getExpiryDate().isBefore(today())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A validade do lote não pode estar no passado.");
    }
    User currentUser = getCurrentUser();

    StockBatch batch =
        StockBatch.builder()
            .medication(med)
            .batchNumber(req.getBatchNumber())
            .expiryDate(req.getExpiryDate())
            .quantityReceived(req.getQuantity())
            .quantityAvailable(req.getQuantity())
            .unitCost(req.getUnitCost())
            .supplier(req.getSupplier())
            .createdBy(currentUser)
            .build();

    StockBatch saved = batchRepository.save(batch);

    // Registar movimento de entrada
    movementRepository.save(
        StockMovement.builder()
            .batch(saved)
            .movementType(MovementType.IN)
            .quantity(req.getQuantity())
            .reason("Stock received from supplier")
            .performedBy(currentUser)
            .build());

    log.info("Medication stock received");
    return toBatchResponse(saved);
  }

  // ------------------------------------------------
  // Dispensing (FEFO)
  // ------------------------------------------------

  @Transactional
  public DispenseResponse dispense(DispenseRequest req) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    Medication med = findMedicationOrThrow(hospitalId, req.getMedicationId());
    if (!med.isActive()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Não é possível dispensar um medicamento inactivo.");
    }
    LocalDate today = today();
    List<StockBatch> batches = batchRepository.findAvailableBatchesFefo(med.getId(), today);
    long totalAvailable = batches.stream().mapToLong(StockBatch::getQuantityAvailable).sum();
    if (totalAvailable < req.getQuantity()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Stock insuficiente. Disponível: "
              + totalAvailable
              + ", solicitado: "
              + req.getQuantity());
    }

    var patient =
        req.getPatientId() != null
            ? patientRepository
                .findById(req.getPatientId())
                .orElseThrow(() -> new EntityNotFoundException("Patient not found"))
            : null;
    UUID episodeId = req.getEpisodeId();
    if (episodeId != null) {
      UUID episodePatientId = episodeApplicationService.getPatientId(episodeId);
      if (patient != null && !episodePatientId.equals(patient.getId())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "O episódio não pertence ao paciente indicado.");
      }
      if (patient == null) {
        patient =
            patientRepository
                .findById(episodePatientId)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found"));
      }
    }

    User currentUser = getCurrentUser();
    int remaining = req.getQuantity();

    for (StockBatch batch : batches) {
      if (remaining <= 0) break;

      int takeFromBatch = Math.min(remaining, batch.getQuantityAvailable());
      batch.setQuantityAvailable(batch.getQuantityAvailable() - takeFromBatch);
      batchRepository.save(batch);

      movementRepository.save(
          StockMovement.builder()
              .batch(batch)
              .movementType(MovementType.DISPENSE)
              .quantity(-takeFromBatch)
              .patient(patient)
              .episodeId(episodeId)
              .reason(req.getReason())
              .performedBy(currentUser)
              .build());

      remaining -= takeFromBatch;
    }

    int newTotal = batchRepository.getTotalAvailableQuantity(med.getId(), today);
    log.info("Medication dispensed");

    return DispenseResponse.builder()
        .medicationId(med.getId())
        .medicationName(med.getName())
        .quantityDispensed(req.getQuantity())
        .remainingStock(newTotal)
        .build();
  }

  @Transactional(readOnly = true)
  public List<StockBatchResponse> findExpiringSoon(int days) {
    if (days < 1 || days > 365) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "O prazo deve estar entre 1 e 365 dias.");
    }
    UUID hospitalId = TenantContext.getCurrentHospital();
    LocalDate today = today();
    return batchRepository.findExpiringSoon(hospitalId, today, today.plusDays(days)).stream()
        .map(this::toBatchResponse)
        .collect(Collectors.toList());
  }

  // ------------------------------------------------
  // Helpers
  // ------------------------------------------------

  private User getCurrentUser() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new EntityNotFoundException("Authenticated user not found");
    }
    return userRepository
        .findByUsername(authentication.getName())
        .orElseThrow(() -> new EntityNotFoundException("Authenticated user not found"));
  }

  private Medication findMedicationOrThrow(UUID hospitalId, UUID medicationId) {
    return medicationRepository
        .findByHospitalIdAndId(hospitalId, medicationId)
        .orElseThrow(() -> new EntityNotFoundException("Medication not found"));
  }

  private LocalDate today() {
    return LocalDate.now(ANGOLA_ZONE);
  }

  private MedicationResponse toMedicationResponse(Medication m) {
    int total = batchRepository.getTotalAvailableQuantity(m.getId(), today());
    return MedicationResponse.builder()
        .id(m.getId())
        .name(m.getName())
        .genericName(m.getGenericName())
        .dosageForm(m.getDosageForm())
        .strength(m.getStrength())
        .unit(m.getUnit())
        .requiresPrescription(m.isRequiresPrescription())
        .minStockLevel(m.getMinStockLevel())
        .totalAvailable(total)
        .active(m.isActive())
        .build();
  }

  private StockBatchResponse toBatchResponse(StockBatch b) {
    return StockBatchResponse.builder()
        .id(b.getId())
        .medicationId(b.getMedication().getId())
        .medicationName(b.getMedication().getName())
        .batchNumber(b.getBatchNumber())
        .expiryDate(b.getExpiryDate())
        .quantityReceived(b.getQuantityReceived())
        .quantityAvailable(b.getQuantityAvailable())
        .unitCost(b.getUnitCost())
        .supplier(b.getSupplier())
        .expired(b.isExpired())
        .expiringSoon(b.isExpiringWithin(30))
        .receivedAt(b.getReceivedAt())
        .build();
  }
}
