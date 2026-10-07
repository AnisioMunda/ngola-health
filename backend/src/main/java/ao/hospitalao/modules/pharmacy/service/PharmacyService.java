package ao.hospitalao.modules.pharmacy.service;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.episodes.repository.EpisodeRepository;
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
public class PharmacyService {

  private final MedicationRepository medicationRepository;
  private final StockBatchRepository batchRepository;
  private final StockMovementRepository movementRepository;
  private final PatientRepository patientRepository;
  private final EpisodeRepository episodeRepository;
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
    log.info("Medication created: {} ({})", saved.getName(), saved.getId());
    return toMedicationResponse(saved);
  }

  // ------------------------------------------------
  // Stock Batches
  // ------------------------------------------------

  @Transactional(readOnly = true)
  public List<StockBatchResponse> findBatchesByMedication(UUID medicationId) {
    return batchRepository.findByMedicationId(medicationId).stream()
        .map(this::toBatchResponse)
        .collect(Collectors.toList());
  }

  @Transactional
  public StockBatchResponse receiveStock(ReceiveStockRequest req) {
    Medication med =
        medicationRepository
            .findById(req.getMedicationId())
            .orElseThrow(() -> new EntityNotFoundException("Medication not found"));

    StockBatch batch =
        StockBatch.builder()
            .medication(med)
            .batchNumber(req.getBatchNumber())
            .expiryDate(req.getExpiryDate())
            .quantityReceived(req.getQuantity())
            .quantityAvailable(req.getQuantity())
            .unitCost(req.getUnitCost())
            .supplier(req.getSupplier())
            .createdBy(getCurrentUser())
            .build();

    StockBatch saved = batchRepository.save(batch);

    // Registar movimento de entrada
    movementRepository.save(
        StockMovement.builder()
            .batch(saved)
            .movementType(MovementType.IN)
            .quantity(req.getQuantity())
            .reason("Stock received from supplier")
            .performedBy(getCurrentUser())
            .build());

    log.info(
        "Stock received: {} units of {} (batch {})",
        req.getQuantity(),
        med.getName(),
        saved.getBatchNumber());
    return toBatchResponse(saved);
  }

  // ------------------------------------------------
  // Dispensing (FEFO)
  // ------------------------------------------------

  @Transactional
  public DispenseResponse dispense(DispenseRequest req) {
    Medication med =
        medicationRepository
            .findById(req.getMedicationId())
            .orElseThrow(() -> new EntityNotFoundException("Medication not found"));

    int totalAvailable = batchRepository.getTotalAvailableQuantity(med.getId(), LocalDate.now());
    if (totalAvailable < req.getQuantity()) {
      throw new IllegalStateException(
          "Insufficient stock. Available: " + totalAvailable + ", requested: " + req.getQuantity());
    }

    var patient =
        req.getPatientId() != null
            ? patientRepository.findById(req.getPatientId()).orElse(null)
            : null;
    var episode =
        req.getEpisodeId() != null
            ? episodeRepository.findById(req.getEpisodeId()).orElse(null)
            : null;

    // FEFO: consumir dos lotes com validade mais próxima primeiro
    List<StockBatch> batches =
        batchRepository.findAvailableBatchesFefo(med.getId(), LocalDate.now());
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
              .episode(episode)
              .reason(req.getReason())
              .performedBy(getCurrentUser())
              .build());

      remaining -= takeFromBatch;
    }

    int newTotal = batchRepository.getTotalAvailableQuantity(med.getId(), LocalDate.now());
    log.info(
        "Dispensed {} units of {} to patient {}",
        req.getQuantity(),
        med.getName(),
        patient != null ? patient.getFullName() : "N/A");

    return DispenseResponse.builder()
        .medicationId(med.getId())
        .medicationName(med.getName())
        .quantityDispensed(req.getQuantity())
        .remainingStock(newTotal)
        .build();
  }

  @Transactional(readOnly = true)
  public List<StockBatchResponse> findExpiringSoon(int days) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    return batchRepository
        .findExpiringSoon(hospitalId, LocalDate.now(), LocalDate.now().plusDays(days))
        .stream()
        .map(this::toBatchResponse)
        .collect(Collectors.toList());
  }

  // ------------------------------------------------
  // Helpers
  // ------------------------------------------------

  private User getCurrentUser() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return userRepository.findByUsername(username).orElse(null);
  }

  private MedicationResponse toMedicationResponse(Medication m) {
    int total = batchRepository.getTotalAvailableQuantity(m.getId(), LocalDate.now());
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
