package ao.hospitalao.modules.episodes.dto;

import ao.hospitalao.modules.episodes.entity.Episode.EpisodeStatus;
import ao.hospitalao.modules.episodes.entity.Episode.EpisodeType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class EpisodeResponse {
    private UUID id;

    // Patient summary
    private UUID patientId;
    private String patientName;

    // Doctor summary
    private UUID doctorId;
    private String doctorName;

    private EpisodeType episodeType;
    private EpisodeStatus status;

    // Scheduling
    private OffsetDateTime scheduledAt;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;

    // Clinical
    private String reason;
    private String symptoms;
    private String diagnosis;
    private String prescription;
    private String notes;

    // Vitals
    private String bloodPressure;
    private Integer heartRate;
    private BigDecimal temperature;
    private BigDecimal weightKg;

    private OffsetDateTime createdAt;
}