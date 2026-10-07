package ao.hospitalao.modules.inpatient.controller;

import ao.hospitalao.modules.inpatient.dto.InpatientDtos.*;
import ao.hospitalao.modules.inpatient.entity.Admission.AdmissionStatus;
import ao.hospitalao.modules.inpatient.service.AdmissionService;
import ao.hospitalao.modules.inpatient.service.BedTransferService;
import ao.hospitalao.modules.inpatient.service.DischargeService;
import ao.hospitalao.modules.inpatient.service.InpatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/inpatient")
@RequiredArgsConstructor
@Tag(name = "Inpatient", description = "Gestão de internamentos, enfermarias e camas")
@SecurityRequirement(name = "bearerAuth")
public class InpatientController {

  private final InpatientService inpatientService;
  private final AdmissionService admissionService;
  private final DischargeService dischargeService;
  private final BedTransferService bedTransferService;

  // ------------------------------------------------
  // Enfermarias
  // ------------------------------------------------

  @GetMapping("/wards")
  @Operation(summary = "Listar todas as enfermarias")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<WardResponse>> findAllWards() {
    return ResponseEntity.ok(inpatientService.findAllWards());
  }

  @PostMapping("/wards")
  @Operation(summary = "Criar enfermaria")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).MANAGER.name())")
  public ResponseEntity<WardResponse> createWard(@Valid @RequestBody CreateWardRequest request) {
    WardResponse created = inpatientService.createWard(request);
    URI uri =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.getId())
            .toUri();
    return ResponseEntity.created(uri).body(created);
  }

  // ------------------------------------------------
  // Mapa de camas por enfermaria
  // ------------------------------------------------

  @GetMapping("/wards/{wardId}/map")
  @Operation(summary = "Mapa visual de camas de uma enfermaria")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<WardMapResponse> getWardMap(@PathVariable UUID wardId) {
    return ResponseEntity.ok(inpatientService.getWardMap(wardId));
  }

  // ------------------------------------------------
  // Camas
  // ------------------------------------------------

  @GetMapping("/wards/{wardId}/beds")
  @Operation(summary = "Listar camas de uma enfermaria")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<BedResponse>> findBedsByWard(@PathVariable UUID wardId) {
    return ResponseEntity.ok(inpatientService.findBedsByWard(wardId));
  }

  @PostMapping("/beds")
  @Operation(summary = "Criar cama")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).MANAGER.name())")
  public ResponseEntity<BedResponse> createBed(@Valid @RequestBody CreateBedRequest request) {
    return ResponseEntity.ok(inpatientService.createBed(request));
  }

  @PatchMapping("/beds/{id}/status")
  @Operation(summary = "Actualizar estado da cama (AVAILABLE/MAINTENANCE/RESERVED)")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).MANAGER.name(),T(ao.hospitalao.security.RoleName).NURSE.name())")
  public ResponseEntity<BedResponse> updateBedStatus(
      @PathVariable UUID id, @Valid @RequestBody UpdateBedStatusRequest request) {
    return ResponseEntity.ok(inpatientService.updateBedStatus(id, request));
  }

  // ------------------------------------------------
  // Internamentos
  // ------------------------------------------------

  @GetMapping("/admissions")
  @Operation(summary = "Listar internamentos com filtros")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Page<AdmissionResponse>> findAll(
      @RequestParam(required = false) AdmissionStatus status,
      @RequestParam(required = false) UUID wardId,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(admissionService.findAll(status, wardId, pageable));
  }

  @GetMapping("/admissions/active")
  @Operation(summary = "Listar todos os internamentos activos")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<AdmissionResponse>> findActive() {
    return ResponseEntity.ok(admissionService.findActiveByHospital());
  }

  @GetMapping("/admissions/{id}")
  @Operation(summary = "Obter internamento por ID")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<AdmissionResponse> findById(@PathVariable UUID id) {
    return ResponseEntity.ok(admissionService.findById(id));
  }

  @PostMapping("/admissions")
  @Operation(summary = "Admitir paciente (criar internamento)")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).NURSE.name(),T(ao.hospitalao.security.RoleName).MANAGER.name(),T(ao.hospitalao.security.RoleName).RECEPTIONIST.name())")
  public ResponseEntity<AdmissionResponse> admit(
      @Valid @RequestBody CreateAdmissionRequest request) {
    AdmissionResponse created = admissionService.admit(request);
    URI uri =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.getId())
            .toUri();
    return ResponseEntity.created(uri).body(created);
  }

  @PatchMapping("/admissions/{id}/discharge")
  @Operation(summary = "Dar alta ao paciente")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).MANAGER.name())")
  public ResponseEntity<AdmissionResponse> discharge(
      @PathVariable UUID id, @Valid @RequestBody DischargeRequest request) {
    return ResponseEntity.ok(dischargeService.discharge(id, request));
  }

  @PatchMapping("/admissions/{id}/transfer")
  @Operation(summary = "Transferir paciente para outra cama/enfermaria")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).NURSE.name(),T(ao.hospitalao.security.RoleName).MANAGER.name())")
  public ResponseEntity<AdmissionResponse> transfer(
      @PathVariable UUID id, @Valid @RequestBody TransferRequest request) {
    return ResponseEntity.ok(bedTransferService.transfer(id, request));
  }
}
