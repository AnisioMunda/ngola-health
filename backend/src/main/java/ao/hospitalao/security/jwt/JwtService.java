package ao.hospitalao.security.jwt;

import ao.hospitalao.config.properties.JwtProperties;
import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.security.RoleName;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

  private static final String TOKEN_TYPE_CLAIM = "token_type";
  private static final String ACCESS_TOKEN_TYPE = "ACCESS";
  private static final String REFRESH_TOKEN_TYPE = "REFRESH";
  private static final String PATIENT_PORTAL_TOKEN_TYPE = "PATIENT_PORTAL";

  private final JwtProperties properties;

  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public UUID extractHospitalId(String token) {
    String hospitalId = extractClaim(token, claims -> claims.get("hospital_id", String.class));
    return hospitalId != null ? UUID.fromString(hospitalId) : null;
  }

  public boolean isPlatformAdminToken(String token) {
    return Boolean.TRUE.equals(
        extractClaim(token, claims -> claims.get("platform_admin", Boolean.class)));
  }

  public boolean isAccessToken(String token) {
    return hasTokenType(token, ACCESS_TOKEN_TYPE);
  }

  public boolean isRefreshToken(String token) {
    return hasTokenType(token, REFRESH_TOKEN_TYPE);
  }

  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  public String generateToken(UserDetails userDetails) {
    return generateToken(new HashMap<>(), userDetails);
  }

  public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>(extraClaims);
    addIdentityClaims(claims, userDetails);
    claims.put(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE);
    return buildToken(claims, userDetails, properties.expirationMs());
  }

  public String generateRefreshToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    addIdentityClaims(claims, userDetails);
    claims.put(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE);
    return buildToken(claims, userDetails, properties.refreshExpirationMs());
  }

  private void addIdentityClaims(Map<String, Object> claims, UserDetails userDetails) {
    if (userDetails instanceof User user) {
      boolean platformAdmin =
          user.getAuthorities().stream()
              .anyMatch(
                  authority -> RoleName.SUPER_ADMIN.authority().equals(authority.getAuthority()));
      if (platformAdmin) {
        claims.put("platform_admin", true);
        claims.remove("hospital_id");
      } else {
        claims.remove("platform_admin");
        if (user.getHospitalId() != null) {
          claims.put("hospital_id", user.getHospitalId().toString());
        } else {
          claims.remove("hospital_id");
        }
      }
    }
  }

  private String buildToken(
      Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
    return Jwts.builder()
        .setClaims(extraClaims)
        .setSubject(userDetails.getUsername())
        .setId(UUID.randomUUID().toString())
        .setIssuedAt(new Date(System.currentTimeMillis()))
        .setExpiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(getSignInKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  public boolean isTokenValid(String token, UserDetails userDetails) {
    final String username = extractUsername(token);
    return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
  }

  private boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  private boolean hasTokenType(String token, String expectedType) {
    return expectedType.equals(
        extractClaim(token, claims -> claims.get(TOKEN_TYPE_CLAIM, String.class)));
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser()
        .verifyWith((SecretKey) getSignInKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  private Key getSignInKey() {
    byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(properties.secret());
    return Keys.hmacShaKeyFor(keyBytes);
  }

  /**
   * Gera token JWT para o portal do paciente. Inclui patient_id e token_type=PATIENT_PORTAL nos
   * claims.
   */
  public String generatePortalToken(UUID patientId, String email, UUID hospitalId) {
    if (hospitalId == null) {
      throw new IllegalArgumentException("A patient portal token requires a hospital scope");
    }

    Map<String, Object> claims = new HashMap<>();
    claims.put("patient_id", patientId.toString());
    claims.put(TOKEN_TYPE_CLAIM, PATIENT_PORTAL_TOKEN_TYPE);
    claims.put("email", email);
    claims.put("hospital_id", hospitalId.toString());

    return Jwts.builder()
        .setClaims(claims)
        .setSubject(email)
        .setIssuedAt(new Date(System.currentTimeMillis()))
        .setExpiration(new Date(System.currentTimeMillis() + properties.expirationMs()))
        .signWith(getSignInKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  /** Verifica se um token é do portal do paciente. */
  public boolean isPatientPortalToken(String token) {
    return hasTokenType(token, PATIENT_PORTAL_TOKEN_TYPE);
  }

  /** Extrai o patient_id do token do portal. */
  public UUID extractPatientId(String token) {
    String patientId = extractClaim(token, claims -> claims.get("patient_id", String.class));
    return patientId != null ? UUID.fromString(patientId) : null;
  }
}
