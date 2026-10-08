package ao.hospitalao.modules.episodes.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.episodes.dto.EpisodeResponse;
import ao.hospitalao.modules.episodes.entity.Episode;
import ao.hospitalao.modules.episodes.entity.Episode.EpisodeStatus;
import ao.hospitalao.modules.episodes.mapper.EpisodeMapper;
import ao.hospitalao.modules.episodes.repository.EpisodeRepository;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EpisodeServiceTest {

  @Mock private EpisodeRepository episodeRepository;
  @Mock private PatientRepository patientRepository;
  @Mock private UserRepository userRepository;
  @Mock private EpisodeMapper episodeMapper;

  @InjectMocks private EpisodeService episodeService;

  @Test
  void scheduledEpisodeCanStartAndCompleteInOrder() {
    UUID episodeId = UUID.randomUUID();
    Episode episode = Episode.builder().id(episodeId).status(EpisodeStatus.SCHEDULED).build();
    when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));
    when(episodeRepository.save(episode)).thenReturn(episode);
    when(episodeMapper.toResponse(episode)).thenAnswer(invocation -> responseFor(episode));

    EpisodeResponse started = episodeService.start(episodeId);

    assertSame(EpisodeStatus.IN_PROGRESS, started.getStatus());
    assertNotNull(episode.getStartedAt());

    EpisodeResponse completed = episodeService.complete(episodeId);

    assertSame(EpisodeStatus.COMPLETED, completed.getStatus());
    assertNotNull(episode.getCompletedAt());
    verify(episodeRepository, org.mockito.Mockito.times(2)).save(episode);
  }

  @Test
  void startingAnEpisodeThatIsNotScheduledIsRejected() {
    assertInvalidTransition(EpisodeStatus.IN_PROGRESS, () -> episodeService.start(id));
  }

  @Test
  void completingAnEpisodeThatIsNotInProgressIsRejected() {
    assertInvalidTransition(EpisodeStatus.SCHEDULED, () -> episodeService.complete(id));
  }

  @Test
  void completedEpisodeCannotBeCancelled() {
    assertInvalidTransition(EpisodeStatus.COMPLETED, () -> episodeService.cancel(id));
  }

  private final UUID id = UUID.randomUUID();

  private void assertInvalidTransition(EpisodeStatus current, Runnable transition) {
    Episode episode = Episode.builder().id(id).status(current).build();
    when(episodeRepository.findById(id)).thenReturn(Optional.of(episode));

    assertThrows(IllegalStateException.class, transition::run);
    verify(episodeRepository, never()).save(episode);
  }

  private EpisodeResponse responseFor(Episode episode) {
    return EpisodeResponse.builder().id(episode.getId()).status(episode.getStatus()).build();
  }
}
