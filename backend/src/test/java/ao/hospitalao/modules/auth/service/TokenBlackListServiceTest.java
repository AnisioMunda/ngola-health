package ao.hospitalao.modules.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import ao.hospitalao.config.SchedulingConfiguration;
import ao.hospitalao.modules.auth.repository.TokenBlackListRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@ExtendWith(MockitoExtension.class)
class TokenBlackListServiceTest {

  @Mock private TokenBlackListRepository repository;

  @Test
  void expiredTokensAreScheduledForCleanup() throws Exception {
    TokenBlackListService service = new TokenBlackListService(repository);
    LocalDateTime before = LocalDateTime.now().minusSeconds(1);

    service.clearExpiredTokens();

    LocalDateTime after = LocalDateTime.now().plusSeconds(1);
    ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
    verify(repository).deleteByExpiryDateBefore(cutoff.capture());

    assertThat(cutoff.getValue()).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    assertThat(
            TokenBlackListService.class
                .getMethod("clearExpiredTokens")
                .isAnnotationPresent(Scheduled.class))
        .isTrue();
    assertThat(SchedulingConfiguration.class.isAnnotationPresent(EnableScheduling.class)).isTrue();
  }
}
