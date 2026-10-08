package ao.hospitalao.security.tenant;

import java.util.UUID;

/** Stores the current hospital scope or an explicit platform-level access grant per thread. */
public class TenantContext {

  private static final ThreadLocal<UUID> currentHospital = new ThreadLocal<>();
  private static final ThreadLocal<Boolean> platformAccess = new ThreadLocal<>();

  private TenantContext() {}

  public static void setCurrentHospital(UUID hospitalId) {
    platformAccess.remove();
    currentHospital.set(hospitalId);
  }

  public static void setPlatformAccess() {
    currentHospital.remove();
    platformAccess.set(true);
  }

  public static UUID getCurrentHospital() {
    return currentHospital.get();
  }

  public static boolean hasPlatformAccess() {
    return Boolean.TRUE.equals(platformAccess.get());
  }

  public static void clear() {
    currentHospital.remove();
    platformAccess.remove();
  }
}
