package ao.hospitalao.modules.hr.service;

import ao.hospitalao.modules.hr.dto.HrDtos.HrStatsDto;
import ao.hospitalao.modules.hr.dto.HrDtos.LeaveRequestResponse;
import ao.hospitalao.modules.hr.dto.HrDtos.ShiftResponse;
import ao.hospitalao.modules.hr.entity.AttendanceRecord.AttendanceStatus;
import ao.hospitalao.modules.hr.entity.LeaveRequest.LeaveStatus;
import ao.hospitalao.modules.hr.entity.Shift;
import ao.hospitalao.modules.hr.repository.AttendanceRepository;
import ao.hospitalao.modules.hr.repository.LeaveRequestRepository;
import ao.hospitalao.modules.hr.repository.ShiftRepository;
import ao.hospitalao.security.tenant.TenantContext;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HrDashboardService {

  private final ShiftRepository shiftRepository;
  private final LeaveRequestRepository leaveRequestRepository;
  private final AttendanceRepository attendanceRepository;
  private final HrService hrService;

  @Transactional(readOnly = true)
  public HrStatsDto getStats() {
    UUID hospitalId = TenantContext.getCurrentHospital();
    LocalDate today = LocalDate.now();
    List<Shift> shifts = shiftRepository.findByHospitalAndPeriod(hospitalId, today, today);
    List<ShiftResponse> todayShifts = shifts.stream().map(hrService::toShiftResponse).toList();
    List<LeaveRequestResponse> pendingLeaves =
        leaveRequestRepository.findByHospitalAndStatus(hospitalId, LeaveStatus.PENDING).stream()
            .map(hrService::toLeaveResponse)
            .toList();

    return HrStatsDto.builder()
        .totalStaff(hrService.countActiveStaff(hospitalId))
        .shiftsToday(shifts.size())
        .pendingLeaves(pendingLeaves.size())
        .presentToday(
            attendanceRepository.countByStatus(hospitalId, today, today, AttendanceStatus.PRESENT))
        .absentToday(
            attendanceRepository.countByStatus(hospitalId, today, today, AttendanceStatus.ABSENT))
        .onLeaveToday(
            attendanceRepository.countByStatus(hospitalId, today, today, AttendanceStatus.ON_LEAVE))
        .todayShifts(todayShifts)
        .pendingLeaveRequests(pendingLeaves)
        .build();
  }
}
