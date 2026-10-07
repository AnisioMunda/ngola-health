package ao.hospitalao.modules.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.auth.dto.AuthResponse;
import ao.hospitalao.modules.auth.entity.Role;
import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.auth.mapper.AuthMapper;
import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.security.RoleName;
import ao.hospitalao.security.jwt.JwtService;
import ao.hospitalao.security.tenant.TenantContext;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtService jwtService;

  @Mock
  private org.springframework.security.authentication.AuthenticationManager authenticationManager;

  @Mock private TokenBlackListService tokenBlackListService;
  @Mock private AuthMapper authMapper;
  @Mock private HospitalRepository hospitalRepository;

  @InjectMocks private AuthService authService;

  @AfterEach
  void clearTenantContext() {
    TenantContext.clear();
  }

  @Test
  void refreshRevokesTheUsedTokenAndReturnsAReplacementPair() {
    UUID hospitalId = UUID.randomUUID();
    Date expiration = new Date(System.currentTimeMillis() + 60_000);
    User user = hospitalAdmin(hospitalId);
    AuthResponse expected = new AuthResponse();
    when(jwtService.isRefreshToken("refresh-old")).thenReturn(true);
    when(tokenBlackListService.isBlacklisted("refresh-old")).thenReturn(false);
    when(jwtService.extractUsername("refresh-old")).thenReturn(user.getUsername());
    when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
    when(jwtService.isPlatformAdminToken("refresh-old")).thenReturn(false);
    when(jwtService.extractHospitalId("refresh-old")).thenReturn(hospitalId);
    when(jwtService.isTokenValid("refresh-old", user)).thenReturn(true);
    when(jwtService.extractExpiration("refresh-old")).thenReturn(expiration);
    when(jwtService.generateToken(user)).thenReturn("access-new");
    when(jwtService.generateRefreshToken(user)).thenReturn("refresh-new");
    when(authMapper.toAuthResponse(user, "access-new", "refresh-new")).thenReturn(expected);

    AuthResponse result = authService.refreshToken("Bearer refresh-old");

    assertThat(result).isSameAs(expected);
    verify(tokenBlackListService).addToBlacklist("refresh-old", expiration);
    verify(authMapper).toAuthResponse(user, "access-new", "refresh-new");
  }

  @Test
  void refreshRejectsAccessTokensAndDoesNotLoadAUser() {
    when(jwtService.isRefreshToken("access-token")).thenReturn(false);

    assertThatThrownBy(() -> authService.refreshToken("Bearer access-token"))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage("Invalid refresh token");

    verify(userRepository, never()).findByUsername(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void refreshRejectsPreviouslyUsedRefreshTokens() {
    when(jwtService.isRefreshToken("refresh-old")).thenReturn(true);
    when(tokenBlackListService.isBlacklisted("refresh-old")).thenReturn(true);

    assertThatThrownBy(() -> authService.refreshToken("Bearer refresh-old"))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage("Invalid refresh token");

    verify(userRepository, never()).findByUsername(org.mockito.ArgumentMatchers.anyString());
  }

  private User hospitalAdmin(UUID hospitalId) {
    User user =
        User.builder()
            .username("hospital-admin")
            .passwordHash("encoded")
            .roles(Set.of(Role.builder().name(RoleName.ADMIN.name()).build()))
            .build();
    user.setHospitalId(hospitalId);
    return user;
  }
}
