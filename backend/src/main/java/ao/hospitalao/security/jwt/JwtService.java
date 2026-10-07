package ao.hospitalao.security.jwt;

import ao.hospitalao.config.properties.JwtProperties;
import ao.hospitalao.modules.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import javax.crypto.SecretKey;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties properties;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public UUID extractHospitalId(String token) {
        String hospitalId = extractClaim(token, claims -> claims.get("hospital_id", String.class));
        return hospitalId != null ? UUID.fromString(hospitalId) : null;
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
        // Adicionar hospital_id ao token se o utilizador tiver hospital
        if (userDetails instanceof User user && user.getHospital() != null) {
            extraClaims.put("hospital_id", user.getHospital().getId().toString());
        }
        return buildToken(extraClaims, userDetails, properties.expirationMs());
    }

    private String buildToken(
        Map<String, Object> extraClaims,
        UserDetails userDetails,
        long expiration
    ) {
        return Jwts.builder()
            .setClaims(extraClaims)
            .setSubject(userDetails.getUsername())
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
     * Gera token JWT para o portal do paciente.
     * Inclui patient_id e token_type=PATIENT_PORTAL nos claims.
     */
    public String generatePortalToken(UUID patientId, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("patient_id",  patientId.toString());
        claims.put("token_type",  "PATIENT_PORTAL");
        claims.put("email",       email);
 
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(email)
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + properties.expirationMs()))
            .signWith(getSignInKey(), SignatureAlgorithm.HS256)
            .compact();
    }
 
    /**
     * Verifica se um token é do portal do paciente.
     */
    public boolean isPatientPortalToken(String token) {
        try {
            String tokenType = extractClaim(token,
                claims -> claims.get("token_type", String.class));
            return "PATIENT_PORTAL".equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }
 
    /**
     * Extrai o patient_id do token do portal.
     */
    public UUID extractPatientId(String token) {
        String patientId = extractClaim(token,
            claims -> claims.get("patient_id", String.class));
        return patientId != null ? UUID.fromString(patientId) : null;
    }
}