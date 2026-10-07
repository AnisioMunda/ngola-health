package ao.hospitalao.modules.pharmacy.dto;

import ao.hospitalao.modules.pharmacy.entity.Medication.DosageForm;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 200)
    private String genericName;

    @NotNull private DosageForm dosageForm;

    @Size(max = 50)
    private String strength;

    @NotBlank
    @Size(max = 20)
    private String unit;

    private boolean requiresPrescription;

    @Min(0)
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
    @NotNull private UUID medicationId;

    @NotBlank
    @Size(max = 50)
    private String batchNumber;

    @NotNull private LocalDate expiryDate;

    @NotNull
    @Min(1)
    private Integer quantity;

    @DecimalMin("0.00")
    private BigDecimal unitCost;

    @Size(max = 200)
    private String supplier;
  }

  // ============================================================
  // Dispensing
  // ============================================================

  @Data
  public static class DispenseRequest {
    @NotNull private UUID medicationId;

    @NotNull
    @Min(1)
    private Integer quantity;

    private UUID patientId;

    private UUID episodeId;

    @Size(max = 500)
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
