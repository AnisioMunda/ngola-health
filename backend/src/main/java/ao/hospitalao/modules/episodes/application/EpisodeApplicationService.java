package ao.hospitalao.modules.episodes.application;

import ao.hospitalao.modules.episodes.repository.EpisodeRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EpisodeApplicationService {

  private final EpisodeRepository episodeRepository;

  @Transactional(readOnly = true)
  public UUID getPatientId(UUID episodeId) {
    return episodeRepository
        .findById(episodeId)
        .map(episode -> episode.getPatientId())
        .orElseThrow(() -> new EntityNotFoundException("Episode not found"));
  }
}
