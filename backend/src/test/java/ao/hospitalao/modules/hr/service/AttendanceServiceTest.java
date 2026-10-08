package ao.hospitalao.modules.hr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.hr.dto.HrDtos.AttendanceResponse;
import ao.hospitalao.modules.hr.dto.HrDtos.CheckOutRequest;
import ao.hospitalao.modules.hr.entity.AttendanceRecord;
import ao.hospitalao.modules.hr.entity.AttendanceRecord.AttendanceStatus;
import ao.hospitalao.modules.hr.repository.AttendanceRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AttendanceServiceTest {

  @Test
  void checksOutAndCalculatesWorkedAndOvertimeMinutes() {
    UUID userId = UUID.randomUUID();
    AttendanceRecord record =
        AttendanceRecord.builder()
            .id(UUID.randomUUID())
            .workDate(LocalDate.now())
            .checkIn(OffsetDateTime.now().minusHours(9))
            .status(AttendanceStatus.PRESENT)
            .build();
    AttendanceRepository repository = mock(AttendanceRepository.class);
    when(repository.findByUserIdAndWorkDate(userId, LocalDate.now()))
        .thenReturn(Optional.of(record));
    when(repository.save(record)).thenReturn(record);
    HrService hrService = mock(HrService.class);
    when(hrService.toAttendanceResponse(record))
        .thenAnswer(
            invocation ->
                AttendanceResponse.builder()
                    .checkOut(record.getCheckOut())
                    .minutesWorked(record.getMinutesWorked())
                    .overtimeMinutes(record.getOvertimeMinutes())
                    .statusLabel("Presente")
                    .build());
    AttendanceService service = new AttendanceService(repository, hrService);

    var result = service.checkOut(userId, new CheckOutRequest());

    assertThat(result.getCheckOut()).isNotNull();
    assertThat(result.getMinutesWorked()).isGreaterThan(8 * 60);
    assertThat(result.getOvertimeMinutes()).isBetween(1, 60);
    assertThat(result.getStatusLabel()).isEqualTo("Presente");
  }
}
