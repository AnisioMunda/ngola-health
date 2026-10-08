package ao.hospitalao.modules.prescription.controller;

import ao.hospitalao.modules.prescription.dto.PrescriptionDtos.*;
import ao.hospitalao.modules.prescription.service.PrescriptionService;
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
@RequestMapping("/prescriptions")
@RequiredArgsConstructor
@Tag(name = "Prescriptions", description = "Gestão de prescrições médicas e dispensas")
@SecurityRequirement(name = "bearerAuth")
public class PrescriptionController {

  private final PrescriptionService prescriptionService;

  // ------------------------------------------------
  // Stats
  // ------------------------------------------------

  @GetMapping("/stats")
  @Operation(summary = "Estatísticas de prescrições")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).MANAGER.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).PHARMACIST.name())")
  public ResponseEntity<PrescriptionStatsDto> getStats() {
    return ResponseEntity.ok(prescriptionService.getStats());
  }

  // ------------------------------------------------
  // CRUD
  // ------------------------------------------------

  @GetMapping
  @Operation(summary = "Listar prescrições do hospital num período")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).MANAGER.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).PHARMACIST.name())")
  public ResponseEntity<Page<PrescriptionResponse>> findAll(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(prescriptionService.findAll(from, to, pageable));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Obter prescrição por ID")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).MANAGER.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).PHARMACIST.name(),T(ao.hospitalao.security.RoleName).NURSE.name())")
  public ResponseEntity<PrescriptionResponse> findById(@PathVariable UUID id) {
    return ResponseEntity.ok(prescriptionService.findById(id));
  }

  @GetMapping("/patient/{patientId}")
  @Operation(summary = "Historial de prescrições de um paciente")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).MANAGER.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).PHARMACIST.name(),T(ao.hospitalao.security.RoleName).NURSE.name())")
  public ResponseEntity<Page<PrescriptionResponse>> findByPatient(
      @PathVariable UUID patientId, @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(prescriptionService.findByPatient(patientId, pageable));
  }

  @GetMapping("/episode/{episodeId}")
  @Operation(summary = "Prescrições de um episódio")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).MANAGER.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).PHARMACIST.name(),T(ao.hospitalao.security.RoleName).NURSE.name())")
  public ResponseEntity<List<PrescriptionResponse>> findByEpisode(@PathVariable UUID episodeId) {
    return ResponseEntity.ok(prescriptionService.findByEpisode(episodeId));
  }

  @GetMapping("/admission/{admissionId}")
  @Operation(summary = "Prescrições de um internamento")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).MANAGER.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).PHARMACIST.name(),T(ao.hospitalao.security.RoleName).NURSE.name())")
  public ResponseEntity<List<PrescriptionResponse>> findByAdmission(
      @PathVariable UUID admissionId) {
    return ResponseEntity.ok(prescriptionService.findByAdmission(admissionId));
  }

  @PostMapping
  @Operation(summary = "Criar prescrição (médico)")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name())")
  public ResponseEntity<PrescriptionResponse> create(
      @Valid @RequestBody CreatePrescriptionRequest request) {
    PrescriptionResponse created = prescriptionService.create(request);
    URI uri =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.getId())
            .toUri();
    return ResponseEntity.created(uri).body(created);
  }

  // ------------------------------------------------
  // Dispensa (farmácia)
  // ------------------------------------------------

  @PostMapping("/{id}/dispense")
  @Operation(summary = "Dispensar medicamento de uma prescrição")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).PHARMACIST.name())")
  public ResponseEntity<PrescriptionResponse> dispense(
      @PathVariable UUID id, @Valid @RequestBody DispenseItemRequest request) {
    return ResponseEntity.ok(prescriptionService.dispense(id, request));
  }

  // ------------------------------------------------
  // Cancelar
  // ------------------------------------------------

  @PatchMapping("/{id}/cancel")
  @Operation(summary = "Cancelar prescrição")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).MANAGER.name())")
  public ResponseEntity<PrescriptionResponse> cancel(
      @PathVariable UUID id, @Valid @RequestBody CancelPrescriptionRequest request) {
    return ResponseEntity.ok(prescriptionService.cancel(id, request));
  }
}
