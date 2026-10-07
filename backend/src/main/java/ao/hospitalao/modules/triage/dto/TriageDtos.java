package ao.hospitalao.modules.triage.dto;

import ao.hospitalao.modules.triage.entity.TriageRecord.TriagePriority;
import ao.hospitalao.modules.triage.entity.TriageRecord.TriageStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

public class TriageDtos {

  @Data
  @Builder
  public static class TriageResponse {
    private UUID id;
    private int queueNumber;
    private UUID patientId;
    private String patientName;
    private Integer patientAge;
    private String patientGender;
    private TriagePriority priority;
    private String priorityLabel;
    private String priorityColor;
    private String chiefComplaint;
    // Sinais vitais
    private String bloodPressure;
    private Integer heartRate;
    private BigDecimal temperature;
    private Integer oxygenSaturation;
    private Integer respiratoryRate;
    private BigDecimal weightKg;
    private Integer painScale;
    private String triageNotes;
    private TriageStatus status;
    private String statusLabel;
    private String triagedByName;
    private UUID episodeId;
    private OffsetDateTime triagedAt;
    private OffsetDateTime attendedAt;
    private OffsetDateTime completedAt;
    private long waitingMinutes;
    private boolean overdue;
  }

  @Data
  public static class CreateTriageRequest {
    // Paciente registado (opcional)
    private UUID patientId;
    // Paciente não registado
    private String patientNameTemp;
    private Integer patientAgeTemp;
    private String patientGenderTemp;
    // Triagem
    private TriagePriority priority;

    @NotBlank(message = "A queixa principal é obrigatória.")
    @Size(max = 500, message = "A queixa principal não pode exceder 500 caracteres.")
    private String chiefComplaint;

    // Sinais vitais
    private String bloodPressure;
    private Integer heartRate;
    private BigDecimal temperature;
    private Integer oxygenSaturation;
    private Integer respiratoryRate;
    private BigDecimal weightKg;
    private Integer painScale;
    private String triageNotes;
  }

  @Data
  public static class UpdatePriorityRequest {
    @NotNull(message = "A prioridade é obrigatória.")
    private TriagePriority priority;

    @Size(max = 500, message = "O motivo não pode exceder 500 caracteres.")
    private String reason;
  }

  @Data
  @Builder
  public static class TriageStatsDto {
    private long totalWaiting;
    private long totalToday;
    private long inProgress;
    private long completedToday;
    private long waitingRed;
    private long waitingOrange;
    private long waitingYellow;
    private long waitingGreen;
    private long waitingBlue;
    private double avgWaitingMinutes;
  }
}
