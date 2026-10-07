package ao.hospitalao.modules.laboratory.controller;

import ao.hospitalao.modules.laboratory.dto.LabDtos.*;
import ao.hospitalao.modules.laboratory.entity.LabRequest.RequestStatus;
import ao.hospitalao.modules.laboratory.service.LabService;
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
@RequestMapping("/lab")
@RequiredArgsConstructor
@Tag(name = "Laboratory", description = "Lab tests, requests and results")
@SecurityRequirement(name = "bearerAuth")
public class LabController {

  private final LabService labService;

  // ------------------------------------------------
  // Catalog
  // ------------------------------------------------

  @GetMapping("/tests")
  @Operation(summary = "List available lab tests catalog")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<LabTestResponse>> findAllTests() {
    return ResponseEntity.ok(labService.findAllTests());
  }

  @PostMapping("/tests")
  @Operation(summary = "Add a new lab test to the catalog")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).MANAGER.name(),T(ao.hospitalao.security.RoleName).LAB_TECHNICIAN.name())")
  public ResponseEntity<LabTestResponse> createTest(
      @Valid @RequestBody CreateLabTestRequest request) {
    return ResponseEntity.ok(labService.createTest(request));
  }

  // ------------------------------------------------
  // Requests
  // ------------------------------------------------

  @GetMapping("/requests")
  @Operation(summary = "List lab requests with optional filters")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).NURSE.name(),T(ao.hospitalao.security.RoleName).LAB_TECHNICIAN.name(),T(ao.hospitalao.security.RoleName).MANAGER.name())")
  public ResponseEntity<Page<LabRequestResponse>> findAll(
      @RequestParam(required = false) UUID patientId,
      @RequestParam(required = false) RequestStatus status,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(labService.findAll(patientId, status, pageable));
  }

  @GetMapping("/requests/{id}")
  @Operation(summary = "Get lab request by ID")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).NURSE.name(),T(ao.hospitalao.security.RoleName).LAB_TECHNICIAN.name(),T(ao.hospitalao.security.RoleName).MANAGER.name())")
  public ResponseEntity<LabRequestResponse> findById(@PathVariable UUID id) {
    return ResponseEntity.ok(labService.findById(id));
  }

  @PostMapping("/requests")
  @Operation(summary = "Create a new lab request")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).NURSE.name())")
  public ResponseEntity<LabRequestResponse> create(
      @Valid @RequestBody CreateLabRequestRequest request) {
    LabRequestResponse created = labService.create(request);
    URI uri =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.getId())
            .toUri();
    return ResponseEntity.created(uri).body(created);
  }

  @PatchMapping("/requests/{id}/collect")
  @Operation(summary = "Mark sample as collected")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).NURSE.name(),T(ao.hospitalao.security.RoleName).LAB_TECHNICIAN.name())")
  public ResponseEntity<LabRequestResponse> collect(@PathVariable UUID id) {
    return ResponseEntity.ok(labService.collect(id));
  }

  @PatchMapping("/requests/{id}/start-analysis")
  @Operation(summary = "Start sample analysis")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).LAB_TECHNICIAN.name())")
  public ResponseEntity<LabRequestResponse> startAnalysis(@PathVariable UUID id) {
    return ResponseEntity.ok(labService.startAnalysis(id));
  }

  @PatchMapping("/requests/{requestId}/items/{itemId}/result")
  @Operation(summary = "Submit result for a specific test item")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).LAB_TECHNICIAN.name())")
  public ResponseEntity<LabRequestResponse> submitResult(
      @PathVariable UUID requestId,
      @PathVariable UUID itemId,
      @Valid @RequestBody SubmitResultRequest request) {
    return ResponseEntity.ok(labService.submitItemResult(requestId, itemId, request));
  }

  @PatchMapping("/requests/{id}/cancel")
  @Operation(summary = "Cancel lab request")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).NURSE.name(),T(ao.hospitalao.security.RoleName).LAB_TECHNICIAN.name())")
  public ResponseEntity<LabRequestResponse> cancel(@PathVariable UUID id) {
    return ResponseEntity.ok(labService.cancel(id));
  }
}
