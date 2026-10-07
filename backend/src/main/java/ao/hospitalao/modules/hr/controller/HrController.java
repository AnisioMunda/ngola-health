package ao.hospitalao.modules.hr.controller;

import ao.hospitalao.modules.hr.dto.HrDtos.*;
import ao.hospitalao.modules.hr.service.HrService;
import ao.hospitalao.security.jwt.JwtUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/hr")
@RequiredArgsConstructor
@Tag(name = "HR", description = "Recursos Humanos — turnos, folgas, ponto")
@SecurityRequirement(name = "bearerAuth")
public class HrController {

    private final HrService hrService;

    // ------------------------------------------------
    // Dashboard
    // ------------------------------------------------

    @GetMapping("/stats")
    @Operation(summary = "Estatísticas RH — turnos hoje, ausências, presenças")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<HrStatsDto> getStats() {
        return ResponseEntity.ok(hrService.getStats());
    }

    // ------------------------------------------------
    // Turnos
    // ------------------------------------------------

    @GetMapping("/shifts/weekly")
    @Operation(summary = "Escala semanal de turnos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WeeklyScheduleResponse> getWeeklySchedule(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart
    ) {
        if (weekStart == null) {
            // Início da semana actual (segunda-feira)
            weekStart = LocalDate.now().with(
                WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);
        }
        return ResponseEntity.ok(hrService.getWeeklySchedule(weekStart));
    }

    @GetMapping("/shifts/my")
    @Operation(summary = "Os meus turnos num período")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ShiftResponse>> getMyShifts(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @AuthenticationPrincipal JwtUserDetails userDetails
    ) {
        return ResponseEntity.ok(hrService.getMyShifts(userDetails.getUserId(), from, to));
    }

    @PostMapping("/shifts")
    @Operation(summary = "Criar turno (ADMIN/MANAGER)")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ShiftResponse> createShift(
        @Valid @RequestBody CreateShiftRequest request
    ) {
        ShiftResponse created = hrService.createShift(request);
        URI uri = ServletUriComponentsBuilder
            .fromCurrentRequest().path("/{id}")
            .buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(uri).body(created);
    }

    @PatchMapping("/shifts/{id}/status")
    @Operation(summary = "Actualizar estado do turno")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ShiftResponse> updateShiftStatus(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateShiftStatusRequest request
    ) {
        return ResponseEntity.ok(hrService.updateShiftStatus(id, request));
    }

    @DeleteMapping("/shifts/{id}")
    @Operation(summary = "Eliminar turno")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Void> deleteShift(@PathVariable UUID id) {
        hrService.deleteShift(id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------
    // Pedidos de Folga
    // ------------------------------------------------

    @GetMapping("/leaves/pending")
    @Operation(summary = "Pedidos de folga pendentes (para aprovação)")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<LeaveRequestResponse>> getPendingLeaves() {
        return ResponseEntity.ok(hrService.getPendingLeaves());
    }

    @GetMapping("/leaves/my")
    @Operation(summary = "Os meus pedidos de folga")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<LeaveRequestResponse>> getMyLeaves(
        @AuthenticationPrincipal JwtUserDetails userDetails,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(hrService.getMyLeaves(userDetails.getUserId(), pageable));
    }

    @GetMapping("/leaves/approved")
    @Operation(summary = "Ausências aprovadas num período — para mapa")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<LeaveRequestResponse>> getApprovedLeaves(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(hrService.getApprovedInPeriod(from, to));
    }

    @PostMapping("/leaves")
    @Operation(summary = "Submeter pedido de folga")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveRequestResponse> requestLeave(
        @Valid @RequestBody CreateLeaveRequest request,
        @AuthenticationPrincipal JwtUserDetails userDetails
    ) {
        LeaveRequestResponse created = hrService.requestLeave(
            userDetails.getUserId(), request);
        URI uri = ServletUriComponentsBuilder
            .fromCurrentRequest().path("/{id}")
            .buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(uri).body(created);
    }

    @PatchMapping("/leaves/{id}/approve")
    @Operation(summary = "Aprovar ou rejeitar pedido de folga")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<LeaveRequestResponse> approveLeave(
        @PathVariable UUID id,
        @Valid @RequestBody ApproveLeaveRequest request
    ) {
        return ResponseEntity.ok(hrService.approveLeave(id, request));
    }

    @PatchMapping("/leaves/{id}/cancel")
    @Operation(summary = "Cancelar pedido de folga")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveRequestResponse> cancelLeave(@PathVariable UUID id) {
        return ResponseEntity.ok(hrService.cancelLeave(id));
    }

    // ------------------------------------------------
    // Controlo de Ponto
    // ------------------------------------------------

    @PostMapping("/attendance/check-in")
    @Operation(summary = "Registar entrada (check-in)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttendanceResponse> checkIn(
        @RequestBody CheckInRequest request,
        @AuthenticationPrincipal JwtUserDetails userDetails
    ) {
        return ResponseEntity.ok(hrService.checkIn(userDetails.getUserId(), request));
    }

    @PatchMapping("/attendance/check-out")
    @Operation(summary = "Registar saída (check-out)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttendanceResponse> checkOut(
        @RequestBody CheckOutRequest request,
        @AuthenticationPrincipal JwtUserDetails userDetails
    ) {
        return ResponseEntity.ok(hrService.checkOut(userDetails.getUserId(), request));
    }

    @GetMapping("/attendance/my")
    @Operation(summary = "O meu histórico de ponto")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AttendanceResponse>> getMyAttendance(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @AuthenticationPrincipal JwtUserDetails userDetails
    ) {
        return ResponseEntity.ok(
            hrService.getAttendanceByPeriod(userDetails.getUserId(), from, to));
    }

    @GetMapping("/attendance/daily")
    @Operation(summary = "Presenças do dia (todos os funcionários)")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<AttendanceResponse>> getDailyAttendance(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        if (date == null) date = LocalDate.now();
        return ResponseEntity.ok(hrService.getDailyAttendance(date));
    }
}