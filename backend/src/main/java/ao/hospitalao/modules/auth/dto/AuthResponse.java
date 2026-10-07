package ao.hospitalao.modules.auth.dto;

import lombok.Data;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de resposta ao login bem-sucedido.
 * Contém os tokens JWT e informação básica do utilizador.
 */
@Data
@Schema(description = "Response for authentication and token JWT")
public class AuthResponse {

    @Schema(description = "User UUID", example = "00000000-0000-0000-0000-000000000099")
    private UUID id;

    @Schema(description = "User Full Name", example = "Donald D. Trump")
    private String fullName;

    @Schema(description = "User Username", example = "donald")
    private String username;

    @Schema(description = "User Email", example = "donald.d.trump@hotmail.com")
    private String email;

    @Schema(description = "Token JWT", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "Refresh Token", example = "hhyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;
}