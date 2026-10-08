package ao.hospitalao.modules.hr.service;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.hr.dto.HrDtos.AttendanceResponse;
import ao.hospitalao.modules.hr.dto.HrDtos.CheckInRequest;
import ao.hospitalao.modules.hr.dto.HrDtos.CreateLeaveRequest;
import ao.hospitalao.modules.hr.dto.HrDtos.CreateShiftRequest;
import ao.hospitalao.modules.hr.dto.HrDtos.LeaveRequestResponse;
import ao.hospitalao.modules.hr.dto.HrDtos.ShiftResponse;
import ao.hospitalao.modules.hr.entity.AttendanceRecord;
import ao.hospitalao.modules.hr.entity.AttendanceRecord.AttendanceStatus;
import ao.hospitalao.modules.hr.entity.LeaveRequest;
import ao.hospitalao.modules.hr.entity.LeaveRequest.LeaveStatus;
import ao.hospitalao.modules.hr.entity.Shift;
import ao.hospitalao.modules.hr.entity.Shift.ShiftStatus;
import ao.hospitalao.modules.hr.repository.ShiftRepository;
import ao.hospitalao.modules.inpatient.repository.WardRepository;
import ao.hospitalao.security.tenant.TenantContext;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HrService {

  private final UserRepository userRepository;
  private final HospitalRepository hospitalRepository;
  private final WardRepository wardRepository;
  private final ShiftRepository shiftRepository;

  public long countActiveStaff(UUID hospitalId) {
    return userRepository.countByHospitalIdAndActiveTrue(hospitalId);
  }

  public Shift createShiftRecord(CreateShiftRequest request) {
    Shift shift =
        Shift.builder()
            .hospital(hospitalRepository.getReferenceById(TenantContext.getCurrentHospital()))
            .user(userRepository.getReferenceById(request.getUserId()))
            .shiftType(
                request.getShiftType() != null ? request.getShiftType() : Shift.ShiftType.MORNING)
            .shiftDate(request.getShiftDate())
            .startTime(request.getStartTime())
            .endTime(request.getEndTime())
            .department(request.getDepartment())
            .notes(request.getNotes())
            .createdBy(getCurrentUser())
            .build();
    if (request.getWardId() != null) {
      shift.setWard(wardRepository.getReferenceById(request.getWardId()));
    }
    return shift;
  }

  public LeaveRequest createLeaveRecord(UUID userId, CreateLeaveRequest request, int totalDays) {
    return LeaveRequest.builder()
        .hospital(hospitalRepository.getReferenceById(TenantContext.getCurrentHospital()))
        .user(userRepository.getReferenceById(userId))
        .leaveType(request.getLeaveType())
        .startDate(request.getStartDate())
        .endDate(request.getEndDate())
        .totalDays(totalDays)
        .reason(request.getReason())
        .build();
  }

  public AttendanceRecord createAttendanceRecord(
      UUID userId, CheckInRequest request, LocalDate workDate) {
    AttendanceRecord record =
        AttendanceRecord.builder()
            .hospital(hospitalRepository.getReferenceById(TenantContext.getCurrentHospital()))
            .user(userRepository.getReferenceById(userId))
            .workDate(workDate)
            .checkIn(OffsetDateTime.now())
            .status(AttendanceStatus.PRESENT)
            .notes(request.getNotes())
            .build();
    if (request.getShiftId() != null) {
      record.setShift(shiftRepository.getReferenceById(request.getShiftId()));
    }
    return record;
  }

  public ShiftResponse toShiftResponse(Shift shift) {
    return ShiftResponse.builder()
        .id(shift.getId())
        .userId(shift.getUser().getId())
        .userFullName(shift.getUser().getFullName())
        .userRole(
            shift.getUser().getRoles() != null && !shift.getUser().getRoles().isEmpty()
                ? shift.getUser().getRoles().iterator().next().toString()
                : null)
        .shiftType(shift.getShiftType())
        .shiftTypeLabel(shiftTypeLabel(shift.getShiftType()))
        .shiftDate(shift.getShiftDate())
        .startTime(shift.getStartTime())
        .endTime(shift.getEndTime())
        .durationHours(shift.getDurationHours())
        .department(shift.getDepartment())
        .wardId(shift.getWard() != null ? shift.getWard().getId() : null)
        .wardName(shift.getWard() != null ? shift.getWard().getName() : null)
        .status(shift.getStatus())
        .statusLabel(shiftStatusLabel(shift.getStatus()))
        .notes(shift.getNotes())
        .createdAt(shift.getCreatedAt())
        .build();
  }

  public LeaveRequestResponse toLeaveResponse(LeaveRequest leave) {
    return LeaveRequestResponse.builder()
        .id(leave.getId())
        .userId(leave.getUser().getId())
        .userFullName(leave.getUser().getFullName())
        .leaveType(leave.getLeaveType())
        .leaveTypeLabel(leaveTypeLabel(leave.getLeaveType()))
        .startDate(leave.getStartDate())
        .endDate(leave.getEndDate())
        .totalDays(leave.getTotalDays())
        .reason(leave.getReason())
        .status(leave.getStatus())
        .statusLabel(leaveStatusLabel(leave.getStatus()))
        .approvedByName(leave.getApprovedBy() != null ? leave.getApprovedBy().getFullName() : null)
        .approvedAt(leave.getApprovedAt())
        .rejectionReason(leave.getRejectionReason())
        .createdAt(leave.getCreatedAt())
        .build();
  }

  public AttendanceResponse toAttendanceResponse(AttendanceRecord record) {
    return AttendanceResponse.builder()
        .id(record.getId())
        .userId(record.getUser().getId())
        .userFullName(record.getUser().getFullName())
        .workDate(record.getWorkDate())
        .checkIn(record.getCheckIn())
        .checkOut(record.getCheckOut())
        .minutesWorked(record.getMinutesWorked())
        .hoursWorked(record.getHoursWorked())
        .overtimeMinutes(record.getOvertimeMinutes())
        .status(record.getStatus())
        .statusLabel(attendanceStatusLabel(record.getStatus()))
        .notes(record.getNotes())
        .createdAt(record.getCreatedAt())
        .build();
  }

  private User getCurrentUser() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return userRepository.findByUsername(username).orElse(null);
  }

  public void setApprover(LeaveRequest leave) {
    leave.setApprovedBy(getCurrentUser());
  }

  private String shiftTypeLabel(Shift.ShiftType type) {
    return switch (type) {
      case MORNING -> "Manhã";
      case AFTERNOON -> "Tarde";
      case NIGHT -> "Noite";
      case FULL_DAY -> "Dia Completo";
      case ON_CALL -> "Chamada";
    };
  }

  private String shiftStatusLabel(ShiftStatus status) {
    return switch (status) {
      case SCHEDULED -> "Agendado";
      case CONFIRMED -> "Confirmado";
      case COMPLETED -> "Concluído";
      case CANCELLED -> "Cancelado";
      case SWAPPED -> "Trocado";
    };
  }

  private String leaveTypeLabel(LeaveRequest.LeaveType type) {
    return switch (type) {
      case VACATION -> "Férias";
      case SICK_LEAVE -> "Baixa Médica";
      case PERSONAL -> "Pessoal";
      case MATERNITY -> "Maternidade";
      case PATERNITY -> "Paternidade";
      case BEREAVEMENT -> "Luto";
      case UNPAID -> "Sem Vencimento";
      case COMPENSATORY -> "Compensatória";
    };
  }

  private String leaveStatusLabel(LeaveStatus status) {
    return switch (status) {
      case PENDING -> "Pendente";
      case APPROVED -> "Aprovado";
      case REJECTED -> "Rejeitado";
      case CANCELLED -> "Cancelado";
    };
  }

  private String attendanceStatusLabel(AttendanceStatus status) {
    return switch (status) {
      case PRESENT -> "Presente";
      case ABSENT -> "Ausente";
      case LATE -> "Atrasado";
      case ON_LEAVE -> "Em Licença";
      case HOLIDAY -> "Feriado";
    };
  }
}
