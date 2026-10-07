package ao.hospitalao.modules.scheduling.controller;

import ao.hospitalao.modules.scheduling.dto.SchedulingDtos.*;
import ao.hospitalao.modules.scheduling.entity.Appointment.AppointmentStatus;
import ao.hospitalao.modules.scheduling.service.SchedulingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/scheduling")
@RequiredArgsConstructor
@Tag(name = "Scheduling", description = "Agendamento online de consultas")
@SecurityRequirement(name = "bearerAuth")
public class SchedulingController {

  private final SchedulingService schedulingService;

  // ------------------------------------------------
  // Horários dos médicos
  // ------------------------------------------------

  @GetMapping("/schedules")
  @Operation(summary = "Listar todos os horários do hospital")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<DoctorScheduleResponse>> getHospitalSchedules() {
    return ResponseEntity.ok(schedulingService.getHospitalSchedules());
  }

  @GetMapping("/schedules/doctor/{doctorId}")
  @Operation(summary = "Listar horários de um médico específico")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<DoctorScheduleResponse>> getDoctorSchedules(
      @PathVariable UUID doctorId) {
    return ResponseEntity.ok(schedulingService.getDoctorSchedules(doctorId));
  }

  @PostMapping("/schedules")
  @Operation(summary = "Definir horário de consultas de um médico")
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
  public ResponseEntity<DoctorScheduleResponse> createSchedule(
      @Valid @RequestBody CreateScheduleRequest request) {
    return ResponseEntity.ok(schedulingService.createSchedule(request));
  }

  @DeleteMapping("/schedules/{id}")
  @Operation(summary = "Remover horário de um médico")
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
  public ResponseEntity<Void> deleteSchedule(@PathVariable UUID id) {
    schedulingService.deleteSchedule(id);
    return ResponseEntity.noContent().build();
  }

  // ------------------------------------------------
  // Disponibilidade / Slots
  // ------------------------------------------------

  @GetMapping("/availability")
  @Operation(summary = "Consultar slots disponíveis de um médico num dia")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<DayAvailabilityResponse> getAvailability(
      @RequestParam UUID doctorId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return ResponseEntity.ok(schedulingService.getAvailability(doctorId, date));
  }

  // ------------------------------------------------
  // Calendário
  // ------------------------------------------------

  @GetMapping("/calendar")
  @Operation(summary = "Obter eventos do calendário num intervalo de datas")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<CalendarEventResponse>> getCalendar(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ResponseEntity.ok(schedulingService.getCalendarEvents(from, to));
  }

  // ------------------------------------------------
  // Agendamentos
  // ------------------------------------------------

  @GetMapping("/appointments")
  @Operation(summary = "Listar agendamentos com filtros")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Page<AppointmentResponse>> findAll(
      @RequestParam(required = false) UUID doctorId,
      @RequestParam(required = false) UUID patientId,
      @RequestParam(required = false) AppointmentStatus status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(
        schedulingService.findAll(doctorId, patientId, status, date, pageable));
  }

  @GetMapping("/appointments/{id}")
  @Operation(summary = "Obter agendamento por ID")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<AppointmentResponse> findById(@PathVariable UUID id) {
    return ResponseEntity.ok(schedulingService.findById(id));
  }

  @PostMapping("/appointments")
  @Operation(summary = "Criar novo agendamento")
  @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','RECEPTIONIST','MANAGER')")
  public ResponseEntity<AppointmentResponse> create(
      @Valid @RequestBody CreateAppointmentRequest request) {
    AppointmentResponse created = schedulingService.create(request);
    URI uri =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.getId())
            .toUri();
    return ResponseEntity.created(uri).body(created);
  }

  @PatchMapping("/appointments/{id}/confirm")
  @Operation(summary = "Confirmar agendamento (SCHEDULED → CONFIRMED)")
  @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','RECEPTIONIST','MANAGER')")
  public ResponseEntity<AppointmentResponse> confirm(@PathVariable UUID id) {
    return ResponseEntity.ok(schedulingService.confirm(id));
  }

  @PatchMapping("/appointments/{id}/complete")
  @Operation(summary = "Marcar consulta como realizada (→ COMPLETED)")
  @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','MANAGER')")
  public ResponseEntity<AppointmentResponse> complete(@PathVariable UUID id) {
    return ResponseEntity.ok(schedulingService.complete(id));
  }

  @PatchMapping("/appointments/{id}/cancel")
  @Operation(summary = "Cancelar agendamento")
  @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','RECEPTIONIST','MANAGER')")
  public ResponseEntity<AppointmentResponse> cancel(
      @PathVariable UUID id, @RequestParam String reason) {
    return ResponseEntity.ok(schedulingService.cancel(id, reason));
  }

  @PatchMapping("/appointments/{id}/no-show")
  @Operation(summary = "Registar falta do paciente (NO_SHOW)")
  @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','RECEPTIONIST','MANAGER')")
  public ResponseEntity<AppointmentResponse> noShow(@PathVariable UUID id) {
    return ResponseEntity.ok(schedulingService.noShow(id));
  }

  // ------------------------------------------------
  // Bloqueios de agenda
  // ------------------------------------------------

  @PostMapping("/blocks")
  @Operation(summary = "Bloquear agenda de um médico (férias, ausência, feriado)")
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
  public ResponseEntity<Void> createBlock(@Valid @RequestBody CreateBlockRequest request) {
    schedulingService.createBlock(request);
    return ResponseEntity.noContent().build();
  }
}
