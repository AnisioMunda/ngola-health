package ao.hospitalao.modules.portal.controller;

import ao.hospitalao.modules.portal.dto.PortalDtos.*;
import ao.hospitalao.modules.portal.service.PatientPortalService;
import ao.hospitalao.security.jwt.PatientPortalPrincipal;
import ao.hospitalao.security.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/portal")
@RequiredArgsConstructor
@Tag(name = "Patient Portal", description = "Portal self-service do paciente")
public class PatientPortalController {

  private final PatientPortalService portalService;

  // ------------------------------------------------
  // Público — sem autenticação
  // ------------------------------------------------

  @PostMapping("/register")
  @Operation(summary = "Registar conta no portal")
  public ResponseEntity<PortalLoginResponse> register(
      @Valid @RequestBody PortalRegisterRequest request) {
    TenantContext.setPlatformAccess();
    try {
      return ResponseEntity.ok(portalService.register(request));
    } finally {
      TenantContext.clear();
    }
  }

  @PostMapping("/login")
  @Operation(summary = "Login no portal")
  public ResponseEntity<PortalLoginResponse> login(@Valid @RequestBody PortalLoginRequest request) {
    TenantContext.setPlatformAccess();
    try {
      return ResponseEntity.ok(portalService.login(request));
    } finally {
      TenantContext.clear();
    }
  }

  // ------------------------------------------------
  // Autenticado — ROLE_PATIENT
  // ------------------------------------------------

  @GetMapping("/dashboard")
  @Operation(summary = "Dashboard do paciente")
  @PreAuthorize("hasRole('PATIENT')")
  public ResponseEntity<PortalDashboardDto> getDashboard(Authentication auth) {
    return ResponseEntity.ok(portalService.getDashboard(extractPatientId(auth)));
  }

  @GetMapping("/episodes")
  @Operation(summary = "Histórico de consultas")
  @PreAuthorize("hasRole('PATIENT')")
  public ResponseEntity<List<PortalEpisodeDto>> getEpisodes(Authentication auth) {
    return ResponseEntity.ok(portalService.getEpisodes(extractPatientId(auth)));
  }

  @GetMapping("/prescriptions")
  @Operation(summary = "Prescrições do paciente")
  @PreAuthorize("hasRole('PATIENT')")
  public ResponseEntity<List<PortalPrescriptionDto>> getPrescriptions(Authentication auth) {
    return ResponseEntity.ok(portalService.getPrescriptions(extractPatientId(auth)));
  }

  @GetMapping("/invoices")
  @Operation(summary = "Facturas do paciente")
  @PreAuthorize("hasRole('PATIENT')")
  public ResponseEntity<List<PortalInvoiceDto>> getInvoices(Authentication auth) {
    return ResponseEntity.ok(portalService.getInvoices(extractPatientId(auth)));
  }

  // ------------------------------------------------
  // Helper
  // ------------------------------------------------

  private UUID extractPatientId(Authentication auth) {
    if (auth == null) {
      throw new IllegalStateException("Sem autenticação.");
    }

    Object principal = auth.getPrincipal();
    log.debug("Portal principal type: {}", principal.getClass().getName());

    if (principal instanceof PatientPortalPrincipal p) {
      return p.getPatientId();
    }

    throw new IllegalStateException(
        "Principal inesperado: "
            + principal.getClass().getName()
            + " — esperado PatientPortalPrincipal.");
  }
}
