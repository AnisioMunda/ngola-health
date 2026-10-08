package ao.hospitalao.modules.hr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.hr.dto.HrDtos.ApproveLeaveRequest;
import ao.hospitalao.modules.hr.dto.HrDtos.LeaveRequestResponse;
import ao.hospitalao.modules.hr.entity.LeaveRequest;
import ao.hospitalao.modules.hr.entity.LeaveRequest.LeaveStatus;
import ao.hospitalao.modules.hr.entity.LeaveRequest.LeaveType;
import ao.hospitalao.modules.hr.repository.LeaveRequestRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LeaveServiceTest {

  @Test
  void rejectsPendingLeaveAndReturnsTheReason() {
    UUID leaveId = UUID.randomUUID();
    LeaveRequest leave =
        LeaveRequest.builder()
            .id(leaveId)
            .leaveType(LeaveType.VACATION)
            .startDate(LocalDate.of(2026, 11, 1))
            .endDate(LocalDate.of(2026, 11, 5))
            .totalDays(5)
            .status(LeaveStatus.PENDING)
            .build();
    LeaveRequestRepository repository = mock(LeaveRequestRepository.class);
    when(repository.findById(leaveId)).thenReturn(Optional.of(leave));
    when(repository.save(leave)).thenReturn(leave);
    HrService hrService = mock(HrService.class);
    when(hrService.toLeaveResponse(leave))
        .thenAnswer(
            invocation ->
                LeaveRequestResponse.builder()
                    .status(leave.getStatus())
                    .statusLabel("Rejeitado")
                    .rejectionReason(leave.getRejectionReason())
                    .build());

    ApproveLeaveRequest request = new ApproveLeaveRequest();
    request.setApproved(false);
    request.setRejectionReason("Cobertura insuficiente");
    LeaveService service = new LeaveService(repository, hrService);

    var result = service.approveLeave(leaveId, request);

    assertThat(result.getStatus()).isEqualTo(LeaveStatus.REJECTED);
    assertThat(result.getStatusLabel()).isEqualTo("Rejeitado");
    assertThat(result.getRejectionReason()).isEqualTo("Cobertura insuficiente");
  }
}
