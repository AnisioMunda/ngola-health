package ao.hospitalao.security.jwt;

import ao.hospitalao.modules.auth.service.TokenBlackListService;
import ao.hospitalao.security.RoleName;
import ao.hospitalao.security.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;
  private final ObjectMapper objectMapper;
  private final TokenBlackListService blacklistService;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    try {
      String authHeader = request.getHeader("Authorization");
      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        try {
          if (!authenticateToken(authHeader.substring(7), request, response)) {
            return;
          }
        } catch (ExpiredJwtException e) {
          log.warn("Expired JWT token: {}", e.getMessage());
          handleException(response, HttpStatus.UNAUTHORIZED, "Token expired. Please login again.");
          return;
        } catch (MalformedJwtException e) {
          log.warn("Malformed JWT token: {}", e.getMessage());
          handleException(response, HttpStatus.UNAUTHORIZED, "Invalid token.");
          return;
        } catch (JwtException e) {
          log.warn("JWT validation error: {}", e.getMessage());
          handleException(response, HttpStatus.UNAUTHORIZED, "Invalid token.");
          return;
        } catch (UsernameNotFoundException e) {
          log.warn("User not found: {}", e.getMessage());
          handleException(response, HttpStatus.UNAUTHORIZED, "User not found.");
          return;
        } catch (Exception e) {
          log.warn("Authentication error: {}", e.getMessage());
          handleException(response, HttpStatus.UNAUTHORIZED, "Authentication error.");
          return;
        }
      }

      filterChain.doFilter(request, response);
    } finally {
      TenantContext.clear();
    }
  }

  private boolean authenticateToken(
      String jwt, HttpServletRequest request, HttpServletResponse response) throws IOException {
    if (blacklistService.isBlacklisted(jwt)) {
      log.warn("Access attempt with revoked token");
      handleException(response, HttpStatus.FORBIDDEN, "Token revoked. Please login again.");
      return false;
    }

    String username = jwtService.extractUsername(jwt);
    if (username == null || SecurityContextHolder.getContext().getAuthentication() != null) {
      return true;
    }

    if (isPatientPortalToken(jwt)) {
      authenticatePatientPortal(jwt, username, request);
    } else {
      authenticateInternalUser(jwt, username, request);
    }
    return true;
  }

  // ----------------------------------------------------------------
  // Portal do paciente — usa PatientPortalPrincipal
  // ----------------------------------------------------------------

  private boolean isPatientPortalToken(String token) {
    try {
      String tokenType =
          jwtService.extractClaim(token, claims -> claims.get("token_type", String.class));
      return "PATIENT_PORTAL".equals(tokenType);
    } catch (Exception e) {
      return false;
    }
  }

  private void authenticatePatientPortal(String jwt, String email, HttpServletRequest request) {

    UUID patientId =
        jwtService.extractClaim(
            jwt,
            claims -> {
              String pid = claims.get("patient_id", String.class);
              return pid != null ? UUID.fromString(pid) : null;
            });

    if (patientId == null) {
      log.warn("Portal token sem patient_id para email: {}", email);
      throw new UsernameNotFoundException("Patient portal token has no patient ID");
    }

    UUID hospitalId = jwtService.extractHospitalId(jwt);
    if (hospitalId == null) {
      throw new UsernameNotFoundException("Patient portal token has no hospital scope");
    }
    TenantContext.setCurrentHospital(hospitalId);

    PatientPortalPrincipal principal = new PatientPortalPrincipal(patientId, email);

    UsernamePasswordAuthenticationToken authToken =
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

    SecurityContextHolder.getContext().setAuthentication(authToken);

    log.debug("Portal autenticado: patientId={}, email={}", patientId, email);
  }

  // ----------------------------------------------------------------
  // Utilizador interno
  // ----------------------------------------------------------------

  private void authenticateInternalUser(String jwt, String username, HttpServletRequest request) {

    UUID hospitalId = jwtService.extractHospitalId(jwt);
    boolean platformAdmin = jwtService.isPlatformAdminToken(jwt);
    if (platformAdmin) {
      TenantContext.setPlatformAccess();
    } else if (hospitalId != null) {
      TenantContext.setCurrentHospital(hospitalId);
    } else {
      throw new UsernameNotFoundException("Token has no hospital scope");
    }

    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

    boolean hasPlatformRole =
        userDetails.getAuthorities().stream()
            .anyMatch(
                authority -> RoleName.SUPER_ADMIN.authority().equals(authority.getAuthority()));
    if (!jwtService.isTokenValid(jwt, userDetails) || platformAdmin != hasPlatformRole) {
      throw new UsernameNotFoundException("Token authority does not match the user account");
    }

    if (!platformAdmin) {
      if (!(userDetails instanceof ao.hospitalao.modules.auth.entity.User user)
          || !hospitalId.equals(user.getHospitalId())) {
        throw new UsernameNotFoundException("Token hospital does not match the user account");
      }
    }

    UsernamePasswordAuthenticationToken authToken =
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

    SecurityContextHolder.getContext().setAuthentication(authToken);

    log.debug("Interno autenticado: {} (hospital: {})", username, hospitalId);
  }

  // ----------------------------------------------------------------
  // Rotas que não exigem autenticação JWT neste filtro
  // ----------------------------------------------------------------

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath();
    return path.equals("/auth/login")
        || path.equals("/auth/refresh")
        || path.equals("/portal/register")
        || path.equals("/portal/login")
        || path.startsWith("/swagger-ui")
        || path.startsWith("/v3/api-docs")
        || path.startsWith("/swagger-resources")
        || path.startsWith("/webjars");
  }

  // ----------------------------------------------------------------
  // handleException — igual ao original
  // ----------------------------------------------------------------

  private void handleException(HttpServletResponse response, HttpStatus status, String message)
      throws IOException {

    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    Map<String, Object> error = new HashMap<>();
    error.put("timestamp", LocalDateTime.now().toString());
    error.put("status", status.value());
    error.put("error", status.getReasonPhrase());
    error.put("message", message);

    response.getWriter().write(objectMapper.writeValueAsString(error));
  }
}
