package ao.hospitalao.modules.inpatient.dto;

import ao.hospitalao.modules.inpatient.entity.Admission.AdmissionStatus;
import ao.hospitalao.modules.inpatient.entity.Admission.DischargeCondition;
import ao.hospitalao.modules.inpatient.entity.Bed.BedStatus;
import ao.hospitalao.modules.inpatient.entity.Bed.BedType;
import ao.hospitalao.modules.inpatient.entity.Ward.WardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

public class InpatientDtos {

  // ============================================================
  // Ward DTOs
  // ============================================================

  @Data
  @Builder
  public static class WardResponse {
    private UUID id;
    private String name;
    private String code;
    private WardType type;
    private String typeLabel;
    private String floor;
    private int totalBeds;
    private int availableBeds;
    private int occupiedBeds;
    private String responsibleDoctorName;
    private String notes;
    private boolean active;
    private List<BedResponse> beds;
  }

  @Data
  public static class CreateWardRequest {
    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 20)
    private String code;

    private WardType type;

    @Size(max = 10)
    private String floor;

    private String notes;

    private UUID responsibleDoctorId;
  }

  // ============================================================
  // Bed DTOs
  // ============================================================

  @Data
  @Builder
  public static class BedResponse {
    private UUID id;
    private String bedNumber;
    private BedStatus status;
    private String statusLabel;
    private BedType type;
    private String wardName;
    private UUID wardId;
    private String notes;
    // Paciente internado (se OCCUPIED)
    private String patientName;
    private UUID admissionId;
    private OffsetDateTime admissionDate;
  }

  @Data
  public static class CreateBedRequest {
    @NotNull private UUID wardId;

    @NotBlank
    @Size(max = 10)
    private String bedNumber;

    private BedType type;

    @Size(max = 300)
    private String notes;
  }

  @Data
  public static class UpdateBedStatusRequest {
    @NotNull private BedStatus status;

    @Size(max = 300)
    private String notes;
  }

  // ============================================================
  // Admission DTOs
  // ============================================================

  @Data
  @Builder
  public static class AdmissionResponse {
    private UUID id;
    private UUID patientId;
    private String patientName;
    private String patientPhone;
    private UUID bedId;
    private String bedNumber;
    private UUID wardId;
    private String wardName;
    private String wardType;
    private UUID doctorId;
    private String doctorName;
    private AdmissionStatus status;
    private String statusLabel;
    private OffsetDateTime admissionDate;
    private LocalDate expectedDischargeDate;
    private OffsetDateTime dischargeDate;
    private String admissionReason;
    private String diagnosis;
    private String dischargeNotes;
    private DischargeCondition dischargeCondition;
    private long daysAdmitted;
    private String admittedByName;
    private String dischargedByName;
    private OffsetDateTime createdAt;
    private List<TransferResponse> transfers;
  }

  @Data
  public static class CreateAdmissionRequest {
    @NotNull private UUID patientId;

    @NotNull private UUID bedId;

    private UUID episodeId;

    @NotNull private UUID responsibleDoctorId;

    @NotBlank
    @Size(max = 500)
    private String admissionReason;

    private LocalDate expectedDischargeDate;
  }

  @Data
  public static class DischargeRequest {
    @Size(max = 10000)
    private String dischargeNotes;

    private DischargeCondition dischargeCondition;
  }

  @Data
  public static class TransferRequest {
    @NotNull private UUID toBedId;

    @Size(max = 300)
    private String reason;
  }

  @Data
  @Builder
  public static class TransferResponse {
    private UUID id;
    private String fromBedNumber;
    private String fromWardName;
    private String toBedNumber;
    private String toWardName;
    private String reason;
    private String transferredByName;
    private OffsetDateTime transferredAt;
  }

  // ============================================================
  // Ward Map (mapa visual de camas)
  // ============================================================

  @Data
  @Builder
  public static class WardMapResponse {
    private UUID wardId;
    private String wardName;
    private String wardType;
    private int totalBeds;
    private int availableBeds;
    private int occupiedBeds;
    private int maintenanceBeds;
    private List<BedResponse> beds;
  }
}
