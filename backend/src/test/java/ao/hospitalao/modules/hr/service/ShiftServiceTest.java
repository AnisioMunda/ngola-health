package ao.hospitalao.modules.hr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.hr.dto.HrDtos.ShiftResponse;
import ao.hospitalao.modules.hr.dto.HrDtos.UpdateShiftStatusRequest;
import ao.hospitalao.modules.hr.entity.Shift;
import ao.hospitalao.modules.hr.entity.Shift.ShiftStatus;
import ao.hospitalao.modules.hr.entity.Shift.ShiftType;
import ao.hospitalao.modules.hr.repository.ShiftRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShiftServiceTest {

  @Test
  void updatesShiftStatusAndMapsResponse() {
    UUID shiftId = UUID.randomUUID();
    Shift shift =
        Shift.builder()
            .id(shiftId)
            .shiftType(ShiftType.MORNING)
            .shiftDate(LocalDate.of(2026, 10, 8))
            .startTime(LocalTime.of(8, 0))
            .endTime(LocalTime.of(16, 0))
            .status(ShiftStatus.SCHEDULED)
            .build();
    ShiftRepository shiftRepository = mock(ShiftRepository.class);
    when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(shift));
    when(shiftRepository.save(shift)).thenReturn(shift);
    HrService hrService = mock(HrService.class);
    when(hrService.toShiftResponse(shift))
        .thenReturn(
            ShiftResponse.builder()
                .status(ShiftStatus.CONFIRMED)
                .statusLabel("Confirmado")
                .durationHours(8)
                .build());

    UpdateShiftStatusRequest request = new UpdateShiftStatusRequest();
    request.setStatus(ShiftStatus.CONFIRMED);
    ShiftService service = new ShiftService(shiftRepository, hrService);

    var result = service.updateShiftStatus(shiftId, request);

    assertThat(result.getStatus()).isEqualTo(ShiftStatus.CONFIRMED);
    assertThat(result.getStatusLabel()).isEqualTo("Confirmado");
    assertThat(result.getDurationHours()).isEqualTo(8);
  }
}
