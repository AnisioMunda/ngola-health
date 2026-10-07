package ao.hospitalao.modules.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.auth.dto.AuthRequest;
import ao.hospitalao.modules.auth.dto.AuthResponse;
import ao.hospitalao.modules.auth.dto.ChangePasswordRequest;
import ao.hospitalao.modules.auth.entity.Role;
import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.auth.entity.enums.RegisterStatus;
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
import org.springframework.security.core.Authentication;
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

  @Test
  void logoutRevokesTheRefreshTokenAndItsMatchingAccessToken() {
    String refreshToken = "refresh-current";
    String accessToken = "access-current";
    String username = "hospital-admin";
    Date refreshExpiration = new Date(System.currentTimeMillis() + 60_000);
    Date accessExpiration = new Date(System.currentTimeMillis() + 30_000);
    when(jwtService.isRefreshToken(refreshToken)).thenReturn(true);
    when(jwtService.extractUsername(refreshToken)).thenReturn(username);
    when(tokenBlackListService.isBlacklisted(refreshToken)).thenReturn(false);
    when(jwtService.extractExpiration(refreshToken)).thenReturn(refreshExpiration);
    when(jwtService.isAccessToken(accessToken)).thenReturn(true);
    when(jwtService.extractUsername(accessToken)).thenReturn(username);
    when(tokenBlackListService.isBlacklisted(accessToken)).thenReturn(false);
    when(jwtService.extractExpiration(accessToken)).thenReturn(accessExpiration);

    authService.logout("Bearer " + accessToken, refreshToken);

    verify(tokenBlackListService).addToBlacklist(refreshToken, refreshExpiration);
    verify(tokenBlackListService).addToBlacklist(accessToken, accessExpiration);
  }

  @Test
  void fifthFailedLoginTemporarilyLocksTheAccount() {
    User user = hospitalAdmin(UUID.randomUUID());
    user.setEmail("hospital-admin@example.com");
    user.setRegisterStatus(RegisterStatus.ACTIVE);
    user.setFailedLoginAttempts(4);
    AuthRequest request = new AuthRequest();
    request.setEmail(user.getEmail());
    request.setPassword("wrong-password");
    when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    doThrow(new BadCredentialsException("Invalid credentials"))
        .when(authenticationManager)
        .authenticate(org.mockito.ArgumentMatchers.any(Authentication.class));

    assertThatThrownBy(() -> authService.authenticate(request))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage("Invalid email or password");

    assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
    assertThat(user.getLockedUntil()).isNotNull();
    assertThat(user.isAccountNonLocked()).isFalse();
    verify(userRepository).save(user);
  }

  @Test
  void passwordChangeClearsTheForcedChangeAndLockoutState() {
    User user = hospitalAdmin(UUID.randomUUID());
    user.setPasswordHash("old-hash");
    user.setMustChangePassword(true);
    user.setFailedLoginAttempts(5);
    user.setLockedUntil(java.time.OffsetDateTime.now().plusMinutes(15));
    ChangePasswordRequest request = new ChangePasswordRequest();
    request.setCurrentPassword("temporary-password");
    request.setNewPassword("new-secure-password");
    when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("temporary-password", "old-hash")).thenReturn(true);
    when(passwordEncoder.matches("new-secure-password", "old-hash")).thenReturn(false);
    when(passwordEncoder.encode("new-secure-password")).thenReturn("new-hash");

    authService.changePassword(user.getUsername(), request);

    assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    assertThat(user.isMustChangePassword()).isFalse();
    assertThat(user.getFailedLoginAttempts()).isZero();
    assertThat(user.getLockedUntil()).isNull();
    verify(userRepository).save(user);
  }

  private User hospitalAdmin(UUID hospitalId) {
    User user =
        User.builder()
            .username("hospital-admin")
            .passwordHash("encoded")
            .registerStatus(RegisterStatus.ACTIVE)
            .roles(Set.of(Role.builder().name(RoleName.ADMIN.name()).build()))
            .build();
    user.setHospitalId(hospitalId);
    return user;
  }
}
