package ao.hospitalao.modules.auth.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ao.hospitalao.modules.auth.entity.TokenBlackList;
import ao.hospitalao.modules.auth.repository.TokenBlackListRepository;  

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Transactional
public class TokenBlackListService {

    private final TokenBlackListRepository repository;

    public void addToBlacklist(String token, Date expirationDate) {
        // Converte Date para LocalDateTime
        LocalDateTime expiry = expirationDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        TokenBlackList blacklistedToken = TokenBlackList.builder()
                .token(token)
                .expiryDate(expiry)
                .build();

        repository.save(blacklistedToken);
    }

    public boolean isBlacklisted(String token) {
        return repository.existsByToken(token);
    }

    // Agendamento para limpar o banco toda madrugada (opcional, mas recomendado)
    @Scheduled(cron = "0 0 3 * * ?")
    public void clearExpiredTokens() {
        repository.deleteByExpiryDateBefore(LocalDateTime.now());
    }
}
