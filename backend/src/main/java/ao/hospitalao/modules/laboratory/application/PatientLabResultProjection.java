package ao.hospitalao.modules.laboratory.application;

import ao.hospitalao.modules.laboratory.entity.LabRequest.RequestStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface PatientLabResultProjection {

  UUID getId();

  String getExamName();

  RequestStatus getStatus();

  String getResultValue();

  String getResultUnit();

  String getReferenceRange();

  String getTestReferenceValues();

  String getDoctorName();

  OffsetDateTime getRequestedAt();

  OffsetDateTime getResultAt();
}
