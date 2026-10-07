package ao.hospitalao.modules.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ao.hospitalao.modules.auth.entity.TokenBlackList;

import java.time.LocalDateTime;

public interface TokenBlackListRepository extends JpaRepository<TokenBlackList, Long> {
    boolean existsByToken(String token);

    // Método para limpar o banco de dados de tokens que já expiraram naturalmente
    void deleteByExpiryDateBefore(LocalDateTime now);
}
