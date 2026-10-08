package ao.hospitalao.modules.auth.service;

import ao.hospitalao.exceptions.EmailAlreadyExistsException;
import ao.hospitalao.exceptions.PasswordPolicyException;
import ao.hospitalao.modules.auth.dto.AuthRequest;
import ao.hospitalao.modules.auth.dto.AuthResponse;
import ao.hospitalao.modules.auth.dto.ChangePasswordRequest;
import ao.hospitalao.modules.auth.dto.RegisterRequest;
import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.auth.entity.enums.RegisterStatus;
import ao.hospitalao.modules.auth.mapper.AuthMapper;
import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.hospitals.application.HospitalApplicationService;
import ao.hospitalao.security.RoleName;
import ao.hospitalao.security.jwt.JwtService;
import ao.hospitalao.security.tenant.TenantContext;
import io.jsonwebtoken.JwtException;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

  private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
  private static final Duration ACCOUNT_LOCK_DURATION = Duration.ofMinutes(15);

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;
  private final TokenBlackListService tokenBlackListService;
  private final AuthMapper authMapper;
  private final HospitalApplicationService hospitalApplicationService;

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    log.info("Registration attempt");

    UUID hospitalId = TenantContext.getCurrentHospital();
    if (hospitalId == null) {
      throw new AccessDeniedException("A hospital scope is required to register a user");
    }

    if (userRepository.existsByEmail(request.getEmail())) {
      log.warn("Registration attempt with an existing email");
      throw new EmailAlreadyExistsException("Email " + request.getEmail() + " is already in use");
    }

    User user =
        User.builder()
            .email(request.getEmail().toLowerCase().trim())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .username(request.getUsername())
            .fullName(request.getFullName().trim())
            .registerStatus(RegisterStatus.ACTIVE)
            .mustChangePassword(request.isMustChangePassword())
            .hospital(hospitalApplicationService.getReferenceById(hospitalId))
            .build();
    user.setHospitalId(hospitalId);

    User savedUser = userRepository.save(user);

    String accessToken = jwtService.generateToken(savedUser);
    String refreshToken = jwtService.generateRefreshToken(savedUser);

    log.info("User registered successfully");
    return authMapper.toAuthResponse(savedUser, accessToken, refreshToken);
  }

  public AuthResponse authenticate(AuthRequest request) {
    TenantContext.setPlatformAccess();
    try {
      log.info("Authentication attempt");

      User user =
          userRepository
              .findByEmail(request.getEmail())
              .orElseThrow(() -> new UsernameNotFoundException("User not found"));

      try {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword()));
      } catch (BadCredentialsException e) {
        log.warn("Authentication failed");
        recordFailedLogin(user);
        throw new BadCredentialsException("Invalid email or password");
      } catch (AuthenticationException e) {
        log.warn("Authentication rejected");
        throw new BadCredentialsException("Invalid email or password");
      }

      boolean platformAdmin =
          user.getAuthorities().stream()
              .anyMatch(
                  authority -> RoleName.SUPER_ADMIN.authority().equals(authority.getAuthority()));
      if (!platformAdmin && user.getHospitalId() == null) {
        throw new BadCredentialsException("Invalid email or password");
      }

      user.setLastLogin(OffsetDateTime.now());
      user.setFailedLoginAttempts(0);
      user.setLockedUntil(null);
      userRepository.save(user);

      String accessToken = jwtService.generateToken(user);
      String refreshToken = jwtService.generateRefreshToken(user);

      log.info("Authentication successful");
      return authMapper.toAuthResponse(user, accessToken, refreshToken);
    } finally {
      TenantContext.clear();
    }
  }

  @Transactional
  public void changePassword(String username, ChangePasswordRequest request) {
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
      throw new BadCredentialsException("Invalid current password");
    }
    if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
      throw new PasswordPolicyException("A nova senha deve ser diferente da senha actual");
    }

    user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
    user.setMustChangePassword(false);
    user.setFailedLoginAttempts(0);
    user.setLockedUntil(null);
    userRepository.save(user);
  }

  @Transactional
  public AuthResponse refreshToken(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new BadCredentialsException("Invalid refresh token");
    }

    String refreshToken = authHeader.substring(7);
    if (!jwtService.isRefreshToken(refreshToken)
        || tokenBlackListService.isBlacklisted(refreshToken)) {
      throw new BadCredentialsException("Invalid refresh token");
    }

    TenantContext.setPlatformAccess();
    try {
      String username = jwtService.extractUsername(refreshToken);
      User user =
          userRepository
              .findByUsername(username)
              .orElseThrow(() -> new UsernameNotFoundException("User not found"));

      boolean platformAdmin =
          user.getAuthorities().stream()
              .anyMatch(
                  authority -> RoleName.SUPER_ADMIN.authority().equals(authority.getAuthority()));
      boolean tokenPlatformAdmin = jwtService.isPlatformAdminToken(refreshToken);
      UUID tokenHospitalId = jwtService.extractHospitalId(refreshToken);
      boolean hospitalMatches =
          platformAdmin
              ? tokenHospitalId == null
              : user.getHospitalId() != null
                  && Objects.equals(tokenHospitalId, user.getHospitalId());

      if (!jwtService.isTokenValid(refreshToken, user)
          || !user.isAccountNonLocked()
          || !user.isEnabled()
          || tokenPlatformAdmin != platformAdmin
          || !hospitalMatches) {
        throw new BadCredentialsException("Invalid refresh token");
      }

      tokenBlackListService.addToBlacklist(
          refreshToken, jwtService.extractExpiration(refreshToken));
      String newAccessToken = jwtService.generateToken(user);
      String newRefreshToken = jwtService.generateRefreshToken(user);
      return authMapper.toAuthResponse(user, newAccessToken, newRefreshToken);
    } finally {
      TenantContext.clear();
    }
  }

  @Transactional
  public void logout(String authHeader, String refreshToken) {
    if (refreshToken == null
        || refreshToken.isBlank()
        || !jwtService.isRefreshToken(refreshToken)) {
      throw new BadCredentialsException("Invalid refresh token");
    }

    String username = jwtService.extractUsername(refreshToken);
    if (!tokenBlackListService.isBlacklisted(refreshToken)) {
      tokenBlackListService.addToBlacklist(
          refreshToken, jwtService.extractExpiration(refreshToken));
      log.info("Refresh token revoked");
    }

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String accessToken = authHeader.substring(7);
      try {
        if (jwtService.isAccessToken(accessToken)
            && username.equals(jwtService.extractUsername(accessToken))
            && !tokenBlackListService.isBlacklisted(accessToken)) {
          tokenBlackListService.addToBlacklist(
              accessToken, jwtService.extractExpiration(accessToken));
          log.info("Access token revoked");
        }
      } catch (JwtException exception) {
        log.debug("Skipping invalid or expired access token during logout");
      }
    }
  }

  private void recordFailedLogin(User user) {
    OffsetDateTime now = OffsetDateTime.now();
    if (user.getLockedUntil() != null && !user.getLockedUntil().isAfter(now)) {
      user.setFailedLoginAttempts(0);
      user.setLockedUntil(null);
    }

    if (user.isAccountNonLocked()) {
      int failedAttempts = user.getFailedLoginAttempts() + 1;
      user.setFailedLoginAttempts(failedAttempts);
      if (failedAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
        user.setLockedUntil(now.plus(ACCOUNT_LOCK_DURATION));
        log.warn("Account temporarily locked after failed login attempts");
      }
      userRepository.save(user);
    }
  }
}
