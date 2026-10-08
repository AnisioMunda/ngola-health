package ao.hospitalao.modules.portal.controller;

import ao.hospitalao.modules.portal.dto.PortalDtos.PortalPendingAccountDto;
import ao.hospitalao.modules.portal.service.PatientPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/patient-portal/accounts")
@RequiredArgsConstructor
@Tag(name = "Patient Portal Administration", description = "Aprovação de contas do portal")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize(
    "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).MANAGER.name(),T(ao.hospitalao.security.RoleName).RECEPTIONIST.name())")
public class PatientPortalAdministrationController {

  private final PatientPortalService portalService;

  @GetMapping("/pending")
  @Operation(summary = "Listar pedidos pendentes do hospital autenticado")
  public ResponseEntity<List<PortalPendingAccountDto>> getPendingApprovals() {
    return ResponseEntity.ok(portalService.getPendingApprovals());
  }

  @PatchMapping("/{accountId}/approve")
  @Operation(summary = "Aprovar conta após confirmar a identidade presencialmente")
  public ResponseEntity<Void> approveAccount(@PathVariable UUID accountId) {
    portalService.approveAccount(accountId);
    return ResponseEntity.noContent().build();
  }
}
