package ao.hospitalao.application.inpatient;

import ao.hospitalao.modules.inpatient.dto.InpatientDtos.AdmissionResponse;
import ao.hospitalao.modules.inpatient.dto.InpatientDtos.TransferResponse;
import ao.hospitalao.modules.inpatient.entity.Admission;
import ao.hospitalao.modules.inpatient.entity.Admission.AdmissionStatus;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AdmissionResponseMapper {

  public AdmissionResponse toResponse(Admission admission) {
    List<TransferResponse> transfers =
        admission.getTransfers() != null
            ? admission.getTransfers().stream()
                .map(
                    transfer ->
                        TransferResponse.builder()
                            .id(transfer.getId())
                            .fromBedNumber(transfer.getFromBed().getBedNumber())
                            .fromWardName(transfer.getFromWard().getName())
                            .toBedNumber(transfer.getToBed().getBedNumber())
                            .toWardName(transfer.getToWard().getName())
                            .reason(transfer.getReason())
                            .transferredByName(
                                transfer.getTransferredBy() != null
                                    ? transfer.getTransferredBy().getFullName()
                                    : null)
                            .transferredAt(transfer.getTransferredAt())
                            .build())
                .collect(Collectors.toList())
            : List.of();

    return AdmissionResponse.builder()
        .id(admission.getId())
        .patientId(admission.getPatient().getId())
        .patientName(admission.getPatient().getFullName())
        .patientPhone(admission.getPatient().getPhone())
        .bedId(admission.getBed().getId())
        .bedNumber(admission.getBed().getBedNumber())
        .wardId(admission.getWard().getId())
        .wardName(admission.getWard().getName())
        .wardType(admission.getWard().getType().name())
        .doctorId(admission.getResponsibleDoctor().getId())
        .doctorName(admission.getResponsibleDoctor().getFullName())
        .status(admission.getStatus())
        .statusLabel(admissionStatusLabel(admission.getStatus()))
        .admissionDate(admission.getAdmissionDate())
        .expectedDischargeDate(admission.getExpectedDischargeDate())
        .dischargeDate(admission.getDischargeDate())
        .admissionReason(admission.getAdmissionReason())
        .diagnosis(admission.getDiagnosis())
        .dischargeNotes(admission.getDischargeNotes())
        .dischargeCondition(admission.getDischargeCondition())
        .daysAdmitted(admission.getDaysAdmitted())
        .admittedByName(
            admission.getAdmittedBy() != null ? admission.getAdmittedBy().getFullName() : null)
        .dischargedByName(
            admission.getDischargedBy() != null ? admission.getDischargedBy().getFullName() : null)
        .createdAt(admission.getCreatedAt())
        .transfers(transfers)
        .build();
  }

  private String admissionStatusLabel(AdmissionStatus status) {
    return switch (status) {
      case ACTIVE -> "Internado";
      case DISCHARGED -> "Alta";
      case TRANSFERRED -> "Transferido";
      case DECEASED -> "Óbito";
    };
  }
}
