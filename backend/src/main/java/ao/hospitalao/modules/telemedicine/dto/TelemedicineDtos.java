package ao.hospitalao.modules.telemedicine.dto;

import ao.hospitalao.modules.telemedicine.entity.TelemedicineSession.SessionStatus;
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
    private String roomUrl; // URL para entrar na sala
    private OffsetDateTime scheduledAt;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private Integer durationMinutes;
    private String clinicalNotes;
    private OffsetDateTime createdAt;
  }

  @Data
  public static class CreateSessionRequest {
    private UUID patientId;
    private UUID appointmentId; // opcional
    private OffsetDateTime scheduledAt;
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
  }
}
