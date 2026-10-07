package ao.hospitalao.modules.patients.controller;

import ao.hospitalao.modules.patients.dto.CreatePatientRequest;
import ao.hospitalao.modules.patients.dto.PatientResponse;
import ao.hospitalao.modules.patients.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
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
@RequestMapping("/patients")
@RequiredArgsConstructor
@Tag(name = "Patients", description = "Patient management")
@SecurityRequirement(name = "bearerAuth")
public class PatientController {

  private final PatientService patientService;

  @GetMapping
  @Operation(summary = "List patients (paginated, searchable)")
  @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','NURSE','RECEPTIONIST','MANAGER')")
  public ResponseEntity<Page<PatientResponse>> findAll(
      @RequestParam(required = false) String search,
      @PageableDefault(size = 20, sort = "fullName") Pageable pageable) {
    return ResponseEntity.ok(patientService.findAll(search, pageable));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get patient by ID")
  @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','NURSE','RECEPTIONIST','MANAGER')")
  public ResponseEntity<PatientResponse> findById(@PathVariable UUID id) {
    return ResponseEntity.ok(patientService.findById(id));
  }

  @PostMapping
  @Operation(summary = "Register new patient")
  @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','NURSE','DOCTOR')")
  public ResponseEntity<PatientResponse> create(@Valid @RequestBody CreatePatientRequest request) {
    PatientResponse created = patientService.create(request);
    URI uri =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.getId())
            .toUri();
    return ResponseEntity.created(uri).body(created);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update patient")
  @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','NURSE','DOCTOR')")
  public ResponseEntity<PatientResponse> update(
      @PathVariable UUID id, @Valid @RequestBody CreatePatientRequest request) {
    return ResponseEntity.ok(patientService.update(id, request));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Deactivate patient (soft delete)")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<PatientResponse> deactivate(@PathVariable UUID id) {
    return ResponseEntity.ok(patientService.deactivate(id));
  }
}
