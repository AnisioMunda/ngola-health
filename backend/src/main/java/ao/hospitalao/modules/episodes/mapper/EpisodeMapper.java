package ao.hospitalao.modules.episodes.mapper;

import ao.hospitalao.modules.episodes.dto.EpisodeResponse;
import ao.hospitalao.modules.episodes.entity.Episode;
import org.springframework.stereotype.Component;

@Component
public class EpisodeMapper {

    public EpisodeResponse toResponse(Episode e) {
        return EpisodeResponse.builder()
            .id(e.getId())
            .patientId(e.getPatient().getId())
            .patientName(e.getPatient().getFullName())
            .doctorId(e.getDoctor() != null ? e.getDoctor().getId() : null)
            .doctorName(e.getDoctor() != null ? e.getDoctor().getFullName() : null)
            .episodeType(e.getEpisodeType())
            .status(e.getStatus())
            .scheduledAt(e.getScheduledAt())
            .startedAt(e.getStartedAt())
            .completedAt(e.getCompletedAt())
            .reason(e.getReason())
            .symptoms(e.getSymptoms())
            .diagnosis(e.getDiagnosis())
            .prescription(e.getPrescription())
            .notes(e.getNotes())
            .bloodPressure(e.getBloodPressure())
            .heartRate(e.getHeartRate())
            .temperature(e.getTemperature())
            .weightKg(e.getWeightKg())
            .createdAt(e.getCreatedAt())
            .build();
    }
}