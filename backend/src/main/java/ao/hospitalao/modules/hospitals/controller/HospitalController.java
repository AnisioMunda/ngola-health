package ao.hospitalao.modules.hospitals.controller;

import ao.hospitalao.modules.hospitals.dto.CreateHospitalRequest;
import ao.hospitalao.modules.hospitals.dto.HospitalResponse;
import ao.hospitalao.modules.hospitals.service.HospitalService;
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
@RequestMapping("/hospitals")
@RequiredArgsConstructor
@Tag(name = "Hospitals", description = "Hospital / clinic management")
@SecurityRequirement(name = "bearerAuth")
public class HospitalController {

  private final HospitalService hospitalService;

  @GetMapping
  @Operation(summary = "List all active hospitals")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<HospitalResponse>> findAll() {
    return ResponseEntity.ok(hospitalService.findAll(false));
  }

  @GetMapping("/management")
  @Operation(summary = "List all hospitals, including inactive")
  @PreAuthorize("hasRole(T(ao.hospitalao.security.RoleName).SUPER_ADMIN.name())")
  public ResponseEntity<List<HospitalResponse>> findAllForManagement() {
    return ResponseEntity.ok(hospitalService.findAll(true));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get hospital by ID")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<HospitalResponse> findById(@PathVariable UUID id) {
    return ResponseEntity.ok(hospitalService.findById(id));
  }

  @PostMapping
  @Operation(summary = "Create new hospital")
  @PreAuthorize("hasRole(T(ao.hospitalao.security.RoleName).SUPER_ADMIN.name())")
  public ResponseEntity<HospitalResponse> create(
      @Valid @RequestBody CreateHospitalRequest request) {
    HospitalResponse created = hospitalService.create(request);
    URI uri =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.getId())
            .toUri();
    return ResponseEntity.created(uri).body(created);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update hospital")
  @PreAuthorize("hasRole(T(ao.hospitalao.security.RoleName).SUPER_ADMIN.name())")
  public ResponseEntity<HospitalResponse> update(
      @PathVariable UUID id, @Valid @RequestBody CreateHospitalRequest request) {
    return ResponseEntity.ok(hospitalService.update(id, request));
  }

  @PatchMapping("/{id}/activate")
  @Operation(summary = "Activate hospital")
  @PreAuthorize("hasRole(T(ao.hospitalao.security.RoleName).SUPER_ADMIN.name())")
  public ResponseEntity<HospitalResponse> activate(@PathVariable UUID id) {
    return ResponseEntity.ok(hospitalService.setActive(id, true));
  }

  @PatchMapping("/{id}/deactivate")
  @Operation(summary = "Deactivate hospital")
  @PreAuthorize("hasRole(T(ao.hospitalao.security.RoleName).SUPER_ADMIN.name())")
  public ResponseEntity<HospitalResponse> deactivate(@PathVariable UUID id) {
    return ResponseEntity.ok(hospitalService.setActive(id, false));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Deactivate hospital without deleting its records")
  @PreAuthorize("hasRole(T(ao.hospitalao.security.RoleName).SUPER_ADMIN.name())")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    hospitalService.setActive(id, false);
    return ResponseEntity.noContent().build();
  }
}
