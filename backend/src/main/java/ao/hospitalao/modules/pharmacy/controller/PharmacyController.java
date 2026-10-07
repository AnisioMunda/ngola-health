package ao.hospitalao.modules.pharmacy.controller;

import ao.hospitalao.modules.pharmacy.dto.PharmacyDtos.*;
import ao.hospitalao.modules.pharmacy.service.PharmacyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/pharmacy")
@RequiredArgsConstructor
@Tag(name = "Pharmacy", description = "Medications, stock and dispensing")
@SecurityRequirement(name = "bearerAuth")
public class PharmacyController {

  private final PharmacyService pharmacyService;

  // ------------------------------------------------
  // Medications
  // ------------------------------------------------

  @GetMapping("/medications")
  @Operation(summary = "List medications (paginated, searchable)")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).PHARMACIST.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).NURSE.name(),T(ao.hospitalao.security.RoleName).MANAGER.name())")
  public ResponseEntity<Page<MedicationResponse>> findAllMedications(
      @RequestParam(required = false) String search,
      @PageableDefault(size = 20, sort = "name") Pageable pageable) {
    return ResponseEntity.ok(pharmacyService.findAllMedications(search, pageable));
  }

  @PostMapping("/medications")
  @Operation(summary = "Add new medication to catalog")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).PHARMACIST.name())")
  public ResponseEntity<MedicationResponse> createMedication(
      @Valid @RequestBody CreateMedicationRequest request) {
    return ResponseEntity.ok(pharmacyService.createMedication(request));
  }

  // ------------------------------------------------
  // Stock
  // ------------------------------------------------

  @GetMapping("/medications/{id}/batches")
  @Operation(summary = "List stock batches for a medication")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).PHARMACIST.name(),T(ao.hospitalao.security.RoleName).MANAGER.name())")
  public ResponseEntity<List<StockBatchResponse>> findBatches(@PathVariable UUID id) {
    return ResponseEntity.ok(pharmacyService.findBatchesByMedication(id));
  }

  @PostMapping("/stock/receive")
  @Operation(summary = "Receive new stock batch")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).PHARMACIST.name())")
  public ResponseEntity<StockBatchResponse> receiveStock(
      @Valid @RequestBody ReceiveStockRequest request) {
    return ResponseEntity.ok(pharmacyService.receiveStock(request));
  }

  @GetMapping("/stock/expiring")
  @Operation(summary = "List batches expiring soon")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).PHARMACIST.name(),T(ao.hospitalao.security.RoleName).MANAGER.name())")
  public ResponseEntity<List<StockBatchResponse>> findExpiringSoon(
      @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
    return ResponseEntity.ok(pharmacyService.findExpiringSoon(days));
  }

  // ------------------------------------------------
  // Dispensing
  // ------------------------------------------------

  @PostMapping("/dispense")
  @Operation(summary = "Dispense medication to a patient (FEFO)")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).PHARMACIST.name())")
  public ResponseEntity<DispenseResponse> dispense(@Valid @RequestBody DispenseRequest request) {
    return ResponseEntity.ok(pharmacyService.dispense(request));
  }
}
