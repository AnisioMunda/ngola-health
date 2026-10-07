package ao.hospitalao.security.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HospitalTenantIdentifierResolverTest {

  private final HospitalTenantIdentifierResolver resolver = new HospitalTenantIdentifierResolver();

  @AfterEach
  void clearTenantContext() {
    TenantContext.clear();
  }

  @Test
  void missingHospitalUsesAnUnassignedTenantAndIsNotRoot() {
    assertThat(resolver.resolveCurrentTenantIdentifier())
        .isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    assertThat(resolver.isRoot(resolver.resolveCurrentTenantIdentifier())).isFalse();
  }

  @Test
  void hospitalContextSelectsThatHospitalWithoutRootAccess() {
    UUID hospitalId = UUID.randomUUID();
    TenantContext.setCurrentHospital(hospitalId);

    assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo(hospitalId);
    assertThat(resolver.isRoot(hospitalId)).isFalse();
  }

  @Test
  void platformAccessMustBeSetExplicitly() {
    TenantContext.setPlatformAccess();

    assertThat(resolver.isRoot(resolver.resolveCurrentTenantIdentifier())).isTrue();
  }
}
