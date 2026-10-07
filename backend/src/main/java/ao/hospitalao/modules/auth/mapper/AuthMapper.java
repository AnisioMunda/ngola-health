package ao.hospitalao.modules.auth.mapper;

import ao.hospitalao.modules.auth.dto.AuthResponse;
import ao.hospitalao.modules.auth.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper responsável por converter a entidade User
 * e os tokens JWT no DTO de resposta da autenticação.
 *
 * Centraliza a lógica de mapeamento evitando duplicação
 * no AuthService.
 */
@Component
public class AuthMapper {
    public AuthResponse toAuthResponse(User user, String accessToken, String refreshToken) {
        AuthResponse dto = new AuthResponse();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setAccessToken(accessToken);
        dto.setRefreshToken(refreshToken);
        return dto;
    }
}