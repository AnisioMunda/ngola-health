package ao.hospitalao.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.standard.SpelExpressionParser;

class RoleNameTest {

  @Test
  void roleNamesResolveInMethodSecurityExpressions() {
    assertThat(
            new SpelExpressionParser()
                .parseExpression("T(ao.hospitalao.security.RoleName).ADMIN.name()")
                .getValue(String.class))
        .isEqualTo(RoleName.ADMIN.name());
  }

  @Test
  void exposesSpringSecurityAuthorityForRole() {
    assertThat(RoleName.SUPER_ADMIN.authority()).isEqualTo("ROLE_SUPER_ADMIN");
  }
}
