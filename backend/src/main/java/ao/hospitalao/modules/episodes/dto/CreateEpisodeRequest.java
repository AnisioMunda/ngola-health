package ao.hospitalao.modules.episodes.dto;

import ao.hospitalao.modules.episodes.entity.Episode.EpisodeType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class CreateEpisodeRequest {

    @NotNull(message = "Patient is required")
    private UUID patientId;

    private UUID doctorId;

    @NotNull(message = "Episode type is required")
    private EpisodeType episodeType;

    private OffsetDateTime scheduledAt;
    private String reason;

    // Optional at creation — filled during/after episode
    private String symptoms;
    private String diagnosis;
    private String prescription;
    private String notes;

    // Vitals
    private String bloodPressure;
    private Integer heartRate;
    private BigDecimal temperature;
    private BigDecimal weightKg;
}