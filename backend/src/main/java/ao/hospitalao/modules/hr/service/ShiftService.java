package ao.hospitalao.modules.hr.service;

import ao.hospitalao.modules.hr.dto.HrDtos.CreateShiftRequest;
import ao.hospitalao.modules.hr.dto.HrDtos.DaySchedule;
import ao.hospitalao.modules.hr.dto.HrDtos.ShiftResponse;
import ao.hospitalao.modules.hr.dto.HrDtos.UpdateShiftStatusRequest;
import ao.hospitalao.modules.hr.dto.HrDtos.WeeklyScheduleResponse;
import ao.hospitalao.modules.hr.entity.Shift;
import ao.hospitalao.modules.hr.entity.Shift.ShiftStatus;
import ao.hospitalao.modules.hr.repository.ShiftRepository;
import ao.hospitalao.security.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShiftService {

  private final ShiftRepository shiftRepository;
  private final HrService hrService;

  @Transactional(readOnly = true)
  public WeeklyScheduleResponse getWeeklySchedule(LocalDate weekStart) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    LocalDate weekEnd = weekStart.plusDays(6);
    List<Shift> shifts = shiftRepository.findByHospitalAndPeriod(hospitalId, weekStart, weekEnd);

    List<DaySchedule> days =
        weekStart
            .datesUntil(weekEnd.plusDays(1))
            .map(
                date -> {
                  List<ShiftResponse> dayShifts =
                      shifts.stream()
                          .filter(shift -> shift.getShiftDate().equals(date))
                          .map(hrService::toShiftResponse)
                          .collect(Collectors.toList());
                  long confirmed =
                      dayShifts.stream()
                          .filter(
                              shift ->
                                  shift.getStatus() == ShiftStatus.CONFIRMED
                                      || shift.getStatus() == ShiftStatus.COMPLETED)
                          .count();

                  return DaySchedule.builder()
                      .date(date)
                      .dayLabel(
                          date.getDayOfWeek()
                              .getDisplayName(TextStyle.FULL, new Locale("pt", "PT")))
                      .shifts(dayShifts)
                      .totalShifts(dayShifts.size())
                      .confirmedShifts((int) confirmed)
                      .build();
                })
            .collect(Collectors.toList());

    return WeeklyScheduleResponse.builder()
        .weekStart(weekStart)
        .weekEnd(weekEnd)
        .days(days)
        .build();
  }

  @Transactional(readOnly = true)
  public List<ShiftResponse> getMyShifts(UUID userId, LocalDate from, LocalDate to) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    return shiftRepository.findByUserAndPeriod(hospitalId, userId, from, to).stream()
        .map(hrService::toShiftResponse)
        .toList();
  }

  @Transactional
  public ShiftResponse createShift(CreateShiftRequest request) {
    UUID excludeId = UUID.randomUUID();
    if (shiftRepository.hasConflict(request.getUserId(), request.getShiftDate(), excludeId)) {
      throw new IllegalStateException(
          "Este funcionário já tem um turno agendado para " + request.getShiftDate());
    }

    Shift shift = hrService.createShiftRecord(request);
    return hrService.toShiftResponse(shiftRepository.save(shift));
  }

  @Transactional
  public ShiftResponse updateShiftStatus(UUID id, UpdateShiftStatusRequest request) {
    Shift shift =
        shiftRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Turno não encontrado"));
    shift.setStatus(request.getStatus());
    if (request.getNotes() != null) {
      shift.setNotes(request.getNotes());
    }
    return hrService.toShiftResponse(shiftRepository.save(shift));
  }

  @Transactional
  public void deleteShift(UUID id) {
    Shift shift =
        shiftRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Turno não encontrado"));
    if (shift.getStatus() == ShiftStatus.COMPLETED) {
      throw new IllegalStateException("Não é possível eliminar um turno já concluído.");
    }
    shiftRepository.delete(shift);
  }
}
