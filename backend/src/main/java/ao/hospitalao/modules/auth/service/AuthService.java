package ao.hospitalao.modules.auth.service;

import ao.hospitalao.exceptions.EmailAlreadyExistsException;
import ao.hospitalao.modules.auth.dto.AuthRequest;
import ao.hospitalao.modules.auth.dto.AuthResponse;
import ao.hospitalao.modules.auth.dto.RegisterRequest;
import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.auth.entity.enums.RegisterStatus;
import ao.hospitalao.modules.auth.mapper.AuthMapper;
import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.security.RoleName;
import ao.hospitalao.security.jwt.JwtService;
import ao.hospitalao.security.tenant.TenantContext;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;
  private final TokenBlackListService tokenBlackListService;
  private final AuthMapper authMapper;
  private final HospitalRepository hospitalRepository;

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    log.info("Register attempt for email: {}", request.getEmail());

    UUID hospitalId = TenantContext.getCurrentHospital();
    if (hospitalId == null) {
      throw new AccessDeniedException("A hospital scope is required to register a user");
    }

    if (userRepository.existsByEmail(request.getEmail())) {
      log.warn("Register attempt with existing email: {}", request.getEmail());
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
            .hospital(hospitalRepository.getReferenceById(hospitalId))
            .build();
    user.setHospitalId(hospitalId);

    User savedUser = userRepository.save(user);

    String accessToken = jwtService.generateToken(savedUser);
    String refreshToken = jwtService.generateRefreshToken(savedUser);

    log.info("User registered successfully: {}", savedUser.getEmail());
    return authMapper.toAuthResponse(savedUser, accessToken, refreshToken);
  }

  public AuthResponse authenticate(AuthRequest request) {
    TenantContext.setPlatformAccess();
    try {
      log.info("Authentication attempt for email: {}", request.getEmail());

      User user =
          userRepository
              .findByEmail(request.getEmail())
              .orElseThrow(() -> new UsernameNotFoundException("User not found"));

      try {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword()));
      } catch (BadCredentialsException e) {
        log.warn("Authentication failed for email: {}", request.getEmail());
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
      userRepository.save(user);

      String accessToken = jwtService.generateToken(user);
      String refreshToken = jwtService.generateRefreshToken(user);

      log.info("Authentication successful for: {}", user.getEmail());
      return authMapper.toAuthResponse(user, accessToken, refreshToken);
    } finally {
      TenantContext.clear();
    }
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

  public void logout(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new IllegalArgumentException("Invalid or missing authorization token");
    }

    String token = authHeader.substring(7);

    try {
      Date expirationDate = jwtService.extractExpiration(token);
      if (expirationDate.after(new Date())) {
        tokenBlackListService.addToBlacklist(token, expirationDate);
        log.info("Token added to blacklist");
      }
    } catch (Exception e) {
      log.error("Error processing logout: {}", e.getMessage());
      throw new RuntimeException("Could not process logout. Invalid token.");
    }
  }
}
