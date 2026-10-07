package ao.hospitalao.modules.episodes.service;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.episodes.dto.CreateEpisodeRequest;
import ao.hospitalao.modules.episodes.dto.EpisodeResponse;
import ao.hospitalao.modules.episodes.dto.UpdateEpisodeRequest;
import ao.hospitalao.modules.episodes.entity.Episode;
import ao.hospitalao.modules.episodes.entity.Episode.EpisodeStatus;
import ao.hospitalao.modules.episodes.mapper.EpisodeMapper;
import ao.hospitalao.modules.episodes.repository.EpisodeRepository;
import ao.hospitalao.modules.patients.entity.Patient;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpisodeService {

  private final EpisodeRepository episodeRepository;
  private final PatientRepository patientRepository;
  private final UserRepository userRepository;
  private final EpisodeMapper episodeMapper;

  @Transactional(readOnly = true)
  public Page<EpisodeResponse> findAll(
      UUID patientId, UUID doctorId, EpisodeStatus status, Pageable pageable) {
    return episodeRepository
        .findWithFilters(patientId, doctorId, status, pageable)
        .map(episodeMapper::toResponse);
  }

  @Transactional(readOnly = true)
  public EpisodeResponse findById(UUID id) {
    return episodeRepository
        .findById(id)
        .map(episodeMapper::toResponse)
        .orElseThrow(() -> new EntityNotFoundException("Episode not found: " + id));
  }

  @Transactional
  public EpisodeResponse create(CreateEpisodeRequest request) {
    Patient patient =
        patientRepository
            .findById(request.getPatientId())
            .orElseThrow(
                () -> new EntityNotFoundException("Patient not found: " + request.getPatientId()));

    User doctor = null;
    if (request.getDoctorId() != null) {
      doctor =
          userRepository
              .findById(request.getDoctorId())
              .orElseThrow(
                  () -> new EntityNotFoundException("Doctor not found: " + request.getDoctorId()));
    }

    Episode episode =
        Episode.builder()
            .patient(patient)
            .doctor(doctor)
            .episodeType(request.getEpisodeType())
            .status(EpisodeStatus.SCHEDULED)
            .scheduledAt(request.getScheduledAt())
            .reason(request.getReason())
            .symptoms(request.getSymptoms())
            .diagnosis(request.getDiagnosis())
            .prescription(request.getPrescription())
            .notes(request.getNotes())
            .bloodPressure(request.getBloodPressure())
            .heartRate(request.getHeartRate())
            .temperature(request.getTemperature())
            .weightKg(request.getWeightKg())
            .createdBy(getCurrentUser())
            .build();

    Episode saved = episodeRepository.save(episode);
    log.info("Episode created: {} for patient {}", saved.getId(), patient.getFullName());
    return episodeMapper.toResponse(saved);
  }

  @Transactional
  public EpisodeResponse update(UUID id, UpdateEpisodeRequest request) {
    Episode episode =
        episodeRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Episode not found: " + id));

    if (request.getDoctorId() != null) {
      User doctor =
          userRepository
              .findById(request.getDoctorId())
              .orElseThrow(
                  () -> new EntityNotFoundException("Doctor not found: " + request.getDoctorId()));
      episode.setDoctor(doctor);
    }
    if (request.getReason() != null) episode.setReason(request.getReason());
    if (request.getSymptoms() != null) episode.setSymptoms(request.getSymptoms());
    if (request.getDiagnosis() != null) episode.setDiagnosis(request.getDiagnosis());
    if (request.getPrescription() != null) episode.setPrescription(request.getPrescription());
    if (request.getNotes() != null) episode.setNotes(request.getNotes());
    if (request.getBloodPressure() != null) episode.setBloodPressure(request.getBloodPressure());
    if (request.getHeartRate() != null) episode.setHeartRate(request.getHeartRate());
    if (request.getTemperature() != null) episode.setTemperature(request.getTemperature());
    if (request.getWeightKg() != null) episode.setWeightKg(request.getWeightKg());

    return episodeMapper.toResponse(episodeRepository.save(episode));
  }

  @Transactional
  public EpisodeResponse start(UUID id) {
    Episode episode = getEpisodeOrThrow(id);
    validateTransition(episode.getStatus(), EpisodeStatus.IN_PROGRESS);
    episode.setStatus(EpisodeStatus.IN_PROGRESS);
    episode.setStartedAt(OffsetDateTime.now());
    return episodeMapper.toResponse(episodeRepository.save(episode));
  }

  @Transactional
  public EpisodeResponse complete(UUID id) {
    Episode episode = getEpisodeOrThrow(id);
    validateTransition(episode.getStatus(), EpisodeStatus.COMPLETED);
    episode.setStatus(EpisodeStatus.COMPLETED);
    episode.setCompletedAt(OffsetDateTime.now());
    return episodeMapper.toResponse(episodeRepository.save(episode));
  }

  @Transactional
  public EpisodeResponse cancel(UUID id) {
    Episode episode = getEpisodeOrThrow(id);
    if (episode.getStatus() == EpisodeStatus.COMPLETED) {
      throw new IllegalStateException("Cannot cancel a completed episode.");
    }
    episode.setStatus(EpisodeStatus.CANCELLED);
    return episodeMapper.toResponse(episodeRepository.save(episode));
  }

  // ------------------------------------------------
  // Helpers
  // ------------------------------------------------

  private Episode getEpisodeOrThrow(UUID id) {
    return episodeRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Episode not found: " + id));
  }

  private void validateTransition(EpisodeStatus current, EpisodeStatus next) {
    boolean valid =
        switch (next) {
          case IN_PROGRESS -> current == EpisodeStatus.SCHEDULED;
          case COMPLETED -> current == EpisodeStatus.IN_PROGRESS;
          default -> false;
        };
    if (!valid) {
      throw new IllegalStateException("Invalid transition: " + current + " → " + next);
    }
  }

  private User getCurrentUser() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return userRepository.findByUsername(username).orElse(null);
  }
}
