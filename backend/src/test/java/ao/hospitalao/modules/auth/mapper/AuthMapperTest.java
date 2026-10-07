package ao.hospitalao.modules.auth.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import ao.hospitalao.modules.auth.entity.Role;
import ao.hospitalao.modules.auth.entity.User;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthMapperTest {

  @Test
  void includesSortedRoleNamesInAuthenticationResponse() {
    User user =
        User.builder()
            .id(UUID.randomUUID())
            .fullName("Utilizador de Teste")
            .username("teste")
            .email("teste@example.invalid")
            .mustChangePassword(true)
            .roles(
                Set.of(
                    Role.builder().name("PHARMACIST").active(true).build(),
                    Role.builder().name("ADMIN").active(true).build()))
            .build();

    var response = new AuthMapper().toAuthResponse(user, "access", "refresh");

    assertThat(response.getRoles()).containsExactly("ADMIN", "PHARMACIST");
    assertThat(response.isMustChangePassword()).isTrue();
  }
}
