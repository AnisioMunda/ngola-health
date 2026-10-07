package ao.hospitalao.modules.prescription.dto;

import ao.hospitalao.modules.prescription.entity.Prescription.PrescriptionStatus;
import ao.hospitalao.modules.prescription.entity.PrescriptionItem.ItemStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

public class PrescriptionDtos {

  // ============================================================
  // Prescription DTOs
  // ============================================================

  @Data
  @Builder
  public static class PrescriptionResponse {
    private UUID id;
    private String prescriptionNumber;
    private UUID patientId;
    private String patientName;
    private UUID doctorId;
    private String doctorName;
    private UUID episodeId;
    private UUID admissionId;
    private PrescriptionStatus status;
    private String statusLabel;
    private LocalDate prescriptionDate;
    private LocalDate expiryDate;
    private boolean expired;
    private String diagnosis;
    private String notes;
    private String cancelledReason;
    private List<PrescriptionItemResponse> items;
    private List<DispensationResponse> dispensations;
    private OffsetDateTime createdAt;
  }

  @Data
  public static class CreatePrescriptionRequest {
    @NotNull private UUID patientId;

    private UUID episodeId;
    private UUID admissionId;

    @Size(max = 500)
    private String diagnosis;

    private String notes;

    @Min(1)
    @Max(365)
    private Integer validityDays;

    @NotEmpty @Valid private List<CreatePrescriptionItemRequest> items;
  }

  @Data
  public static class CancelPrescriptionRequest {
    @NotBlank
    @Size(max = 300)
    private String reason;
  }

  // ============================================================
  // Item DTOs
  // ============================================================

  @Data
  @Builder
  public static class PrescriptionItemResponse {
    private UUID id;
    private UUID medicationId;
    private String medicationName;
    private String medicationUnit;
    private int quantityPrescribed;
    private int quantityDispensed;
    private int remainingQuantity;
    private String dosage;
    private Integer frequencyHours;
    private Integer durationDays;
    private String route;
    private String instructions;
    private ItemStatus status;
    private String statusLabel;
    private int stockAvailable; // stock disponível em tempo real
  }

  @Data
  public static class CreatePrescriptionItemRequest {
    @NotNull private UUID medicationId;

    @Min(1)
    private int quantityPrescribed;

    @NotBlank
    @Size(max = 200)
    private String dosage;

    @Min(1)
    private Integer frequencyHours;

    @Min(1)
    private Integer durationDays;

    @Size(max = 50)
    private String route;

    @Size(max = 300)
    private String instructions;
  }

  @Data
  public static class DispenseItemRequest {
    @NotNull private UUID prescriptionItemId;

    @Min(1)
    private int quantityToDispense;

    @Size(max = 300)
    private String notes;
  }

  // ============================================================
  // Dispensation DTOs
  // ============================================================

  @Data
  @Builder
  public static class DispensationResponse {
    private UUID id;
    private UUID prescriptionItemId;
    private String medicationName;
    private String batchNumber;
    private String dispensedByName;
    private int quantityDispensed;
    private OffsetDateTime dispensedAt;
    private String notes;
  }

  // ============================================================
  // Stats
  // ============================================================

  @Data
  @Builder
  public static class PrescriptionStatsDto {
    private long totalActive;
    private long totalToday;
    private long pendingDispense;
    private long expiringSoon; // expira nos próximos 3 dias
  }
}
