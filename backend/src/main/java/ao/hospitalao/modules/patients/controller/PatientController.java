package ao.hospitalao.modules.patients.controller;

import ao.hospitalao.modules.patients.dto.CheckPatientDuplicatesRequest;
import ao.hospitalao.modules.patients.dto.CreatePatientRequest;
import ao.hospitalao.modules.patients.dto.PatientDuplicateCandidateResponse;
import ao.hospitalao.modules.patients.dto.PatientResponse;
import ao.hospitalao.modules.patients.service.PatientService;
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
@RequestMapping("/patients")
@RequiredArgsConstructor
@Tag(name = "Pacientes", description = "Gestão de pacientes")
@SecurityRequirement(name = "bearerAuth")
public class PatientController {

  private final PatientService patientService;

  @GetMapping
  @Operation(summary = "Listar e pesquisar pacientes de forma paginada")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).NURSE.name(),T(ao.hospitalao.security.RoleName).RECEPTIONIST.name(),T(ao.hospitalao.security.RoleName).MANAGER.name())")
  public ResponseEntity<Page<PatientResponse>> findAll(
      @RequestParam(required = false) String search,
      @PageableDefault(size = 20, sort = "fullName") Pageable pageable) {
    return ResponseEntity.ok(patientService.findAll(search, pageable));
  }

  @PostMapping("/possible-duplicates")
  @Operation(summary = "Verificar possíveis duplicados antes do cadastro")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).NURSE.name(),T(ao.hospitalao.security.RoleName).RECEPTIONIST.name(),T(ao.hospitalao.security.RoleName).MANAGER.name())")
  public ResponseEntity<List<PatientDuplicateCandidateResponse>> findPossibleDuplicates(
      @Valid @RequestBody CheckPatientDuplicatesRequest request) {
    return ResponseEntity.ok(patientService.findPossibleDuplicates(request));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Consultar paciente pelo identificador")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).NURSE.name(),T(ao.hospitalao.security.RoleName).RECEPTIONIST.name(),T(ao.hospitalao.security.RoleName).MANAGER.name())")
  public ResponseEntity<PatientResponse> findById(@PathVariable UUID id) {
    return ResponseEntity.ok(patientService.findById(id));
  }

  @PostMapping
  @Operation(summary = "Cadastrar paciente")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).RECEPTIONIST.name(),T(ao.hospitalao.security.RoleName).NURSE.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name())")
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
  @Operation(summary = "Actualizar dados do paciente")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).RECEPTIONIST.name(),T(ao.hospitalao.security.RoleName).NURSE.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name())")
  public ResponseEntity<PatientResponse> update(
      @PathVariable UUID id, @Valid @RequestBody CreatePatientRequest request) {
    return ResponseEntity.ok(patientService.update(id, request));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Inactivar paciente sem apagar o registo")
  @PreAuthorize("hasRole(T(ao.hospitalao.security.RoleName).ADMIN.name())")
  public ResponseEntity<PatientResponse> deactivate(@PathVariable UUID id) {
    return ResponseEntity.ok(patientService.deactivate(id));
  }
}
