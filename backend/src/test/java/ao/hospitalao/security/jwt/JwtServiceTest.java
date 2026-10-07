package ao.hospitalao.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import ao.hospitalao.config.properties.JwtProperties;
import ao.hospitalao.modules.auth.entity.Role;
import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.security.RoleName;
import io.jsonwebtoken.Claims;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private final JwtService jwtService =
      new JwtService(
          new JwtProperties(Base64.getEncoder().encodeToString(new byte[32]), 60_000, 600_000));

  @Test
  void accessAndRefreshTokensHaveDifferentTypesAndLifetimes() {
    User user = userInHospital(UUID.randomUUID());
    String accessToken = jwtService.generateToken(user);
    String refreshToken = jwtService.generateRefreshToken(user);

    assertThat(jwtService.isAccessToken(accessToken)).isTrue();
    assertThat(jwtService.isRefreshToken(accessToken)).isFalse();
    assertThat(jwtService.isRefreshToken(refreshToken)).isTrue();
    assertThat(jwtService.isAccessToken(refreshToken)).isFalse();
    assertThat(jwtService.extractExpiration(refreshToken))
        .isAfter(jwtService.extractExpiration(accessToken));
    assertThat(jwtService.extractHospitalId(refreshToken)).isEqualTo(user.getHospitalId());
  }

  @Test
  void refreshTokensRemainUniqueWhenIssuedForTheSameUser() {
    User user = userInHospital(UUID.randomUUID());

    String firstToken = jwtService.generateRefreshToken(user);
    String secondToken = jwtService.generateRefreshToken(user);
    String firstTokenId = jwtService.extractClaim(firstToken, Claims::getId);
    String secondTokenId = jwtService.extractClaim(secondToken, Claims::getId);

    assertThat(secondToken).isNotEqualTo(firstToken);
    assertThat(firstTokenId).isNotEqualTo(secondTokenId);
  }

  @Test
  void platformAdminTokensDoNotCarryAHospitalScope() {
    User user =
        User.builder()
            .username("platform-admin")
            .passwordHash("encoded")
            .roles(Set.of(Role.builder().name(RoleName.SUPER_ADMIN.name()).build()))
            .build();

    String refreshToken = jwtService.generateRefreshToken(user);

    assertThat(jwtService.isPlatformAdminToken(refreshToken)).isTrue();
    assertThat(jwtService.extractHospitalId(refreshToken)).isNull();
  }

  private User userInHospital(UUID hospitalId) {
    User user =
        User.builder()
            .username("hospital-user")
            .passwordHash("encoded")
            .roles(Set.of(Role.builder().name(RoleName.ADMIN.name()).build()))
            .build();
    user.setHospitalId(hospitalId);
    return user;
  }
}
