package ao.hospitalao.modules.auth.repository;

import ao.hospitalao.modules.auth.entity.TokenBlackList;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenBlackListRepository extends JpaRepository<TokenBlackList, Long> {
  boolean existsByToken(String token);

  // Método para limpar o banco de dados de tokens que já expiraram naturalmente
  void deleteByExpiryDateBefore(LocalDateTime now);
}
