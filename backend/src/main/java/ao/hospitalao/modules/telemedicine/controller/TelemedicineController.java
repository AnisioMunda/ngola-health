package ao.hospitalao.modules.telemedicine.controller;

import ao.hospitalao.modules.telemedicine.dto.TelemedicineDtos.*;
import ao.hospitalao.modules.telemedicine.service.TelemedicineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/telemedicine")
@RequiredArgsConstructor
@Tag(name = "Telemedicine", description = "Videoconsultas e sessões remotas")
@SecurityRequirement(name = "bearerAuth")
public class TelemedicineController {

  private final TelemedicineService telemedicineService;

  @GetMapping("/stats")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).MANAGER.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name())")
  public ResponseEntity<TelemedicineStatsDto> getStats() {
    return ResponseEntity.ok(telemedicineService.getStats());
  }

  @GetMapping("/active")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).MANAGER.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name())")
  public ResponseEntity<List<SessionResponse>> getActiveSessions() {
    return ResponseEntity.ok(telemedicineService.getActiveSessions());
  }

  @GetMapping("/my-sessions")
  @PreAuthorize("hasRole(T(ao.hospitalao.security.RoleName).DOCTOR.name())")
  @Operation(summary = "Sessões do médico autenticado hoje")
  public ResponseEntity<List<SessionResponse>> getMySessionsToday() {
    return ResponseEntity.ok(telemedicineService.getMySessionsToday());
  }

  @GetMapping("/doctors")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).MANAGER.name(),T(ao.hospitalao.security.RoleName).RECEPTIONIST.name())")
  public ResponseEntity<List<DoctorOptionDto>> getTeamsDoctors() {
    return ResponseEntity.ok(telemedicineService.getTeamsDoctors());
  }

  @PostMapping
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).MANAGER.name(),T(ao.hospitalao.security.RoleName).RECEPTIONIST.name())")
  public ResponseEntity<SessionResponse> create(@Valid @RequestBody CreateSessionRequest request) {
    SessionResponse created = telemedicineService.create(request);
    URI uri =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.getId())
            .toUri();
    return ResponseEntity.created(uri).body(created);
  }

  @PatchMapping("/room/{token}/join")
  @Operation(summary = "Médico entra na sala — inicia sessão")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).ADMIN.name())")
  public ResponseEntity<SessionResponse> joinSession(@PathVariable String token) {
    return ResponseEntity.ok(telemedicineService.joinSession(token));
  }

  @PatchMapping("/room/{token}/waiting")
  @Operation(summary = "Paciente entrou na sala de espera (chamado pelo portal)")
  public ResponseEntity<SessionResponse> patientWaiting(@PathVariable String token) {
    return ResponseEntity.ok(telemedicineService.patientWaiting(token));
  }

  @PatchMapping("/{id}/end")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).ADMIN.name())")
  public ResponseEntity<SessionResponse> endSession(
      @PathVariable UUID id, @RequestBody UpdateNotesRequest request) {
    return ResponseEntity.ok(telemedicineService.endSession(id, request));
  }

  @PatchMapping("/{id}/cancel")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).MANAGER.name(),T(ao.hospitalao.security.RoleName).RECEPTIONIST.name())")
  public ResponseEntity<SessionResponse> cancel(@PathVariable UUID id) {
    return ResponseEntity.ok(telemedicineService.cancel(id));
  }
}
