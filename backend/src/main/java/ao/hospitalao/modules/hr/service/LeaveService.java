package ao.hospitalao.modules.hr.service;

import ao.hospitalao.modules.hr.dto.HrDtos.ApproveLeaveRequest;
import ao.hospitalao.modules.hr.dto.HrDtos.CreateLeaveRequest;
import ao.hospitalao.modules.hr.dto.HrDtos.LeaveRequestResponse;
import ao.hospitalao.modules.hr.entity.LeaveRequest;
import ao.hospitalao.modules.hr.entity.LeaveRequest.LeaveStatus;
import ao.hospitalao.modules.hr.repository.LeaveRequestRepository;
import ao.hospitalao.security.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeaveService {

  private final LeaveRequestRepository leaveRequestRepository;
  private final HrService hrService;

  @Transactional(readOnly = true)
  public List<LeaveRequestResponse> getPendingLeaves() {
    UUID hospitalId = TenantContext.getCurrentHospital();
    return leaveRequestRepository.findByHospitalAndStatus(hospitalId, LeaveStatus.PENDING).stream()
        .map(hrService::toLeaveResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public Page<LeaveRequestResponse> getMyLeaves(UUID userId, Pageable pageable) {
    return leaveRequestRepository
        .findByUserIdOrderByCreatedAtDesc(userId, pageable)
        .map(hrService::toLeaveResponse);
  }

  @Transactional(readOnly = true)
  public List<LeaveRequestResponse> getApprovedInPeriod(LocalDate from, LocalDate to) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    return leaveRequestRepository.findApprovedInPeriod(hospitalId, from, to).stream()
        .map(hrService::toLeaveResponse)
        .toList();
  }

  @Transactional
  public LeaveRequestResponse requestLeave(UUID userId, CreateLeaveRequest request) {
    if (request.getEndDate().isBefore(request.getStartDate())) {
      throw new IllegalArgumentException("Data de fim não pode ser anterior à data de início.");
    }

    UUID excludeId = UUID.randomUUID();
    if (leaveRequestRepository.hasOverlap(
        userId, request.getStartDate(), request.getEndDate(), excludeId)) {
      throw new IllegalStateException("Já existe um pedido de ausência para este período.");
    }

    int totalDays =
        (int) (request.getEndDate().toEpochDay() - request.getStartDate().toEpochDay() + 1);
    LeaveRequest leave = hrService.createLeaveRecord(userId, request, totalDays);
    return hrService.toLeaveResponse(leaveRequestRepository.save(leave));
  }

  @Transactional
  public LeaveRequestResponse approveLeave(UUID id, ApproveLeaveRequest request) {
    LeaveRequest leave =
        leaveRequestRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Pedido de folga não encontrado"));
    if (leave.getStatus() != LeaveStatus.PENDING) {
      throw new IllegalStateException("Este pedido já foi processado.");
    }

    if (request.isApproved()) {
      leave.setStatus(LeaveStatus.APPROVED);
      hrService.setApprover(leave);
      leave.setApprovedAt(OffsetDateTime.now());
    } else {
      leave.setStatus(LeaveStatus.REJECTED);
      leave.setRejectionReason(request.getRejectionReason());
    }
    return hrService.toLeaveResponse(leaveRequestRepository.save(leave));
  }

  @Transactional
  public LeaveRequestResponse cancelLeave(UUID id) {
    LeaveRequest leave =
        leaveRequestRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Pedido de folga não encontrado"));
    if (leave.getStatus() == LeaveStatus.APPROVED
        && leave.getStartDate().isBefore(LocalDate.now())) {
      throw new IllegalStateException("Não é possível cancelar uma licença já iniciada.");
    }
    leave.setStatus(LeaveStatus.CANCELLED);
    return hrService.toLeaveResponse(leaveRequestRepository.save(leave));
  }
}
