package ao.hospitalao.modules.hr.service;

import ao.hospitalao.modules.hr.dto.HrDtos.AttendanceResponse;
import ao.hospitalao.modules.hr.dto.HrDtos.CheckInRequest;
import ao.hospitalao.modules.hr.dto.HrDtos.CheckOutRequest;
import ao.hospitalao.modules.hr.entity.AttendanceRecord;
import ao.hospitalao.modules.hr.repository.AttendanceRepository;
import ao.hospitalao.security.tenant.TenantContext;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttendanceService {

  private final AttendanceRepository attendanceRepository;
  private final HrService hrService;

  @Transactional
  public AttendanceResponse checkIn(UUID userId, CheckInRequest request) {
    LocalDate today = LocalDate.now();
    attendanceRepository
        .findByUserIdAndWorkDate(userId, today)
        .ifPresent(
            record -> {
              throw new IllegalStateException("Check-in já registado para hoje.");
            });

    AttendanceRecord record = hrService.createAttendanceRecord(userId, request, today);
    return hrService.toAttendanceResponse(attendanceRepository.save(record));
  }

  @Transactional
  public AttendanceResponse checkOut(UUID userId, CheckOutRequest request) {
    LocalDate today = LocalDate.now();
    AttendanceRecord record =
        attendanceRepository
            .findByUserIdAndWorkDate(userId, today)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Sem check-in registado para hoje. Faça check-in primeiro."));
    if (record.getCheckOut() != null) {
      throw new IllegalStateException("Check-out já registado para hoje.");
    }

    record.setCheckOut(OffsetDateTime.now());
    record.calculateMinutesWorked();
    int standardMinutes = 8 * 60;
    if (record.getMinutesWorked() > standardMinutes) {
      record.setOvertimeMinutes(record.getMinutesWorked() - standardMinutes);
    }
    if (request.getNotes() != null) {
      record.setNotes(request.getNotes());
    }
    return hrService.toAttendanceResponse(attendanceRepository.save(record));
  }

  @Transactional(readOnly = true)
  public List<AttendanceResponse> getAttendanceByPeriod(UUID userId, LocalDate from, LocalDate to) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    return attendanceRepository.findByUserAndPeriod(hospitalId, userId, from, to).stream()
        .map(hrService::toAttendanceResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<AttendanceResponse> getDailyAttendance(LocalDate date) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    return attendanceRepository.findByHospitalAndDate(hospitalId, date).stream()
        .map(hrService::toAttendanceResponse)
        .toList();
  }
}
