package ao.hospitalao.modules.pharmacy.dto;

import ao.hospitalao.modules.pharmacy.entity.Medication.DosageForm;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

public class PharmacyDtos {

  // ============================================================
  // Medication
  // ============================================================

  @Data
  @Builder
  public static class MedicationResponse {
    private UUID id;
    private String name;
    private String genericName;
    private DosageForm dosageForm;
    private String strength;
    private String unit;
    private boolean requiresPrescription;
    private Integer minStockLevel;
    private Integer totalAvailable;
    private boolean active;
  }

  @Data
  public static class CreateMedicationRequest {
    private String name;
    private String genericName;
    private DosageForm dosageForm;
    private String strength;
    private String unit;
    private boolean requiresPrescription;
    private Integer minStockLevel;
  }

  // ============================================================
  // StockBatch
  // ============================================================

  @Data
  @Builder
  public static class StockBatchResponse {
    private UUID id;
    private UUID medicationId;
    private String medicationName;
    private String batchNumber;
    private LocalDate expiryDate;
    private Integer quantityReceived;
    private Integer quantityAvailable;
    private BigDecimal unitCost;
    private String supplier;
    private boolean expired;
    private boolean expiringSoon;
    private OffsetDateTime receivedAt;
  }

  @Data
  public static class ReceiveStockRequest {
    private UUID medicationId;
    private String batchNumber;
    private LocalDate expiryDate;
    private Integer quantity;
    private BigDecimal unitCost;
    private String supplier;
  }

  // ============================================================
  // Dispensing
  // ============================================================

  @Data
  public static class DispenseRequest {
    private UUID medicationId;
    private Integer quantity;
    private UUID patientId;
    private UUID episodeId;
    private String reason;
  }

  @Data
  @Builder
  public static class DispenseResponse {
    private UUID medicationId;
    private String medicationName;
    private Integer quantityDispensed;
    private Integer remainingStock;
  }
}
