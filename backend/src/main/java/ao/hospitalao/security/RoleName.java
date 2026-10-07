package ao.hospitalao.security;

public enum RoleName {
  ADMIN,
  MANAGER,
  DOCTOR,
  NURSE,
  RECEPTIONIST,
  PHARMACIST,
  FINANCIAL,
  LAB_TECHNICIAN,
  SUPER_ADMIN,
  PATIENT;

  public String authority() {
    return "ROLE_" + name();
  }
}
