package ao.hospitalao.security.tenant;

import java.util.UUID;

/**
 * Armazena o hospital_id do utilizador autenticado para a thread actual. Permite que os serviços
 * saibam a que hospital os dados pertencem sem precisar de passar o ID em cada método.
 */
public class TenantContext {

  private static final ThreadLocal<UUID> currentHospital = new ThreadLocal<>();

  private TenantContext() {}

  public static void setCurrentHospital(UUID hospitalId) {
    currentHospital.set(hospitalId);
  }

  public static UUID getCurrentHospital() {
    return currentHospital.get();
  }

  public static void clear() {
    currentHospital.remove();
  }
}
