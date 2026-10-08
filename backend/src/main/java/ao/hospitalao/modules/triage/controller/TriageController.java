package ao.hospitalao.modules.triage.controller;

import ao.hospitalao.modules.triage.dto.TriageDtos.*;
import ao.hospitalao.modules.triage.service.TriageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/triage")
@RequiredArgsConstructor
@Tag(name = "Triage", description = "Triagem e urgência — fila de espera Manchester")
@SecurityRequirement(name = "bearerAuth")
public class TriageController {

  private final TriageService triageService;

  @GetMapping("/stats")
  @Operation(summary = "Estatísticas da urgência em tempo real")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).MANAGER.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).NURSE.name())")
  public ResponseEntity<TriageStatsDto> getStats() {
    return ResponseEntity.ok(triageService.getStats());
  }

  @GetMapping("/queue")
  @Operation(summary = "Fila de espera activa (WAITING + IN_PROGRESS)")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).NURSE.name())")
  public ResponseEntity<List<TriageResponse>> getActiveQueue() {
    return ResponseEntity.ok(triageService.getActiveQueue());
  }

  @GetMapping("/queue/waiting")
  @Operation(summary = "Apenas pacientes em espera")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).NURSE.name())")
  public ResponseEntity<List<TriageResponse>> getWaitingQueue() {
    return ResponseEntity.ok(triageService.getWaitingQueue());
  }

  @GetMapping("/history")
  @Operation(summary = "Histórico de triagens por data")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).NURSE.name())")
  public ResponseEntity<List<TriageResponse>> getHistory(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate date) {
    return ResponseEntity.ok(triageService.getHistory(date));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Detalhe de uma triagem")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).NURSE.name())")
  public ResponseEntity<TriageResponse> findById(@PathVariable UUID id) {
    return ResponseEntity.ok(triageService.findById(id));
  }

  @PostMapping
  @Operation(summary = "Criar triagem")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).NURSE.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name())")
  public ResponseEntity<TriageResponse> create(@Valid @RequestBody CreateTriageRequest request) {
    TriageResponse created = triageService.create(request);
    URI uri =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.getId())
            .toUri();
    return ResponseEntity.created(uri).body(created);
  }

  @PatchMapping("/{id}/priority")
  @Operation(summary = "Actualizar prioridade de triagem")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).NURSE.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name())")
  public ResponseEntity<TriageResponse> updatePriority(
      @PathVariable UUID id, @Valid @RequestBody UpdatePriorityRequest request) {
    return ResponseEntity.ok(triageService.updatePriority(id, request));
  }

  @PatchMapping("/{id}/call")
  @Operation(summary = "Chamar paciente — WAITING → IN_PROGRESS")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).NURSE.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name())")
  public ResponseEntity<TriageResponse> callNext(@PathVariable UUID id) {
    return ResponseEntity.ok(triageService.callNext(id));
  }

  @PatchMapping("/{id}/complete")
  @Operation(summary = "Completar atendimento — IN_PROGRESS → COMPLETED")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).NURSE.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name())")
  public ResponseEntity<TriageResponse> complete(@PathVariable UUID id) {
    return ResponseEntity.ok(triageService.complete(id));
  }

  @PatchMapping("/{id}/left")
  @Operation(summary = "Marcar paciente como saiu sem ser atendido")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).NURSE.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name())")
  public ResponseEntity<TriageResponse> markAsLeft(@PathVariable UUID id) {
    return ResponseEntity.ok(triageService.markAsLeft(id));
  }
}
