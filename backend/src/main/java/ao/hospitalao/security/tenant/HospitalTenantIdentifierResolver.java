package ao.hospitalao.security.tenant;

import java.util.UUID;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class HospitalTenantIdentifierResolver implements CurrentTenantIdentifierResolver<UUID> {

  private static final UUID NO_HOSPITAL = UUID.fromString("00000000-0000-0000-0000-000000000000");

  @Override
  public UUID resolveCurrentTenantIdentifier() {
    UUID currentHospital = TenantContext.getCurrentHospital();
    return currentHospital == null ? NO_HOSPITAL : currentHospital;
  }

  @Override
  public boolean validateExistingCurrentSessions() {
    return true;
  }

  @Override
  public boolean isRoot(UUID tenantIdentifier) {
    return TenantContext.hasPlatformAccess();
  }
}
