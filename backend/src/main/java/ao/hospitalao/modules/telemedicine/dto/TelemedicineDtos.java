package ao.hospitalao.modules.telemedicine.dto;

import ao.hospitalao.modules.telemedicine.entity.TelemedicineSession.SessionStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

public class TelemedicineDtos {

  @Data
  @Builder
  public static class SessionResponse {
    private UUID id;
    private UUID patientId;
    private String patientName;
    private UUID doctorId;
    private String doctorName;
    private UUID appointmentId;
    private SessionStatus status;
    private String statusLabel;
    private String roomToken;
    private String roomUrl;
    private OffsetDateTime scheduledAt;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private Integer durationMinutes;
    private String clinicalNotes;
    private OffsetDateTime createdAt;
  }

  @Data
  public static class CreateSessionRequest {
    @NotNull private UUID patientId;
    @NotNull private UUID doctorId;
    private UUID appointmentId; // opcional
    @NotNull @Future private OffsetDateTime scheduledAt;

    @NotNull
    @Positive
    @Max(1440)
    private Integer durationMinutes;
  }

  @Data
  @Builder
  public static class DoctorOptionDto {
    private UUID id;
    private String fullName;
  }

  @Data
  public static class UpdateNotesRequest {
    private String clinicalNotes;
  }

  @Data
  @Builder
  public static class TelemedicineStatsDto {
    private long totalScheduled;
    private long totalToday;
    private long inProgress;
    private long completedThisMonth;
    private double avgDurationMinutes;
    private boolean teamsConfigured;
  }
}
