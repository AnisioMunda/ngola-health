package ao.hospitalao.modules.equipment.controller;
 
import ao.hospitalao.modules.equipment.dto.EquipmentDtos.*;
import ao.hospitalao.modules.equipment.service.EquipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
 
import java.net.URI;
import java.util.List;
import java.util.UUID;
 
@RestController
@RequestMapping("/equipment")
@RequiredArgsConstructor
@Tag(name = "Equipment", description = "Gestão de equipamentos médicos e manutenção")
@SecurityRequirement(name = "bearerAuth")
public class EquipmentController {
 
    private final EquipmentService equipmentService;
 
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<EquipmentStatsDto> getStats() {
        return ResponseEntity.ok(equipmentService.getStats());
    }
 
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<EquipmentResponse>> findAll(
        @RequestParam(required = false) String q,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(equipmentService.findAll(q, pageable));
    }
 
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EquipmentResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(equipmentService.findById(id));
    }
 
    @GetMapping("/maintenance-due")
    @Operation(summary = "Equipamentos com manutenção em atraso ou nos próximos 7 dias")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<EquipmentResponse>> findMaintenanceDue() {
        return ResponseEntity.ok(equipmentService.findMaintenanceDue());
    }
 
    @GetMapping("/calibration-due")
    @Operation(summary = "Equipamentos com calibração em atraso ou nos próximos 7 dias")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<EquipmentResponse>> findCalibrationDue() {
        return ResponseEntity.ok(equipmentService.findCalibrationDue());
    }
 
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<EquipmentResponse> create(
        @Valid @RequestBody CreateEquipmentRequest request
    ) {
        EquipmentResponse created = equipmentService.create(request);
        URI uri = ServletUriComponentsBuilder
            .fromCurrentRequest().path("/{id}")
            .buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(uri).body(created);
    }
 
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<EquipmentResponse> updateStatus(
        @PathVariable UUID id,
        @RequestBody UpdateStatusRequest request
    ) {
        return ResponseEntity.ok(equipmentService.updateStatus(id, request));
    }
 
    @PostMapping("/{id}/maintenance")
    @Operation(summary = "Registar manutenção / calibração")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<EquipmentResponse> addMaintenance(
        @PathVariable UUID id,
        @Valid @RequestBody CreateMaintenanceRequest request
    ) {
        return ResponseEntity.ok(equipmentService.addMaintenance(id, request));
    }
}