package ao.hospitalao.modules.patients.exception;

public class PatientIdentifierConflictException extends RuntimeException {

  public PatientIdentifierConflictException() {
    super("Patient identifier already registered in this hospital.");
  }
}
