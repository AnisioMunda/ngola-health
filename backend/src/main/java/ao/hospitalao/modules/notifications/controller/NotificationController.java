package ao.hospitalao.modules.notifications.controller;

import ao.hospitalao.modules.notifications.service.NotificationService;
import ao.hospitalao.modules.notifications.service.NotificationService.NotificationDto;
import ao.hospitalao.security.jwt.JwtUserDetails;
import ao.hospitalao.security.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notificações internas do sistema")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping
  @Operation(summary = "Listar notificações do utilizador (paginado)")
  public ResponseEntity<Page<NotificationDto>> getAll(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
      @AuthenticationPrincipal UserDetails userDetails) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    UUID userId = getUserId(userDetails);
    return ResponseEntity.ok(
        notificationService.getForUser(hospitalId, userId, PageRequest.of(page, size)));
  }

  @GetMapping("/unread-count")
  @Operation(summary = "Número de notificações não lidas (para o sino)")
  public ResponseEntity<Map<String, Long>> countUnread(
      @AuthenticationPrincipal UserDetails userDetails) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    UUID userId = getUserId(userDetails);
    long count = notificationService.countUnread(hospitalId, userId);
    return ResponseEntity.ok(Map.of("count", count));
  }

  @GetMapping("/top-unread")
  @Operation(summary = "Últimas 5 notificações não lidas (dropdown do sino)")
  public ResponseEntity<List<NotificationDto>> getTopUnread(
      @AuthenticationPrincipal UserDetails userDetails) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    UUID userId = getUserId(userDetails);
    return ResponseEntity.ok(notificationService.getTopUnread(hospitalId, userId));
  }

  @PatchMapping("/{id}/read")
  @Operation(summary = "Marcar notificação como lida")
  public ResponseEntity<Void> markAsRead(
      @PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    UUID userId = getUserId(userDetails);
    notificationService.markAsRead(id, hospitalId, userId);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/read-all")
  @Operation(summary = "Marcar todas as notificações como lidas")
  public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal UserDetails userDetails) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    UUID userId = getUserId(userDetails);
    notificationService.markAllAsRead(hospitalId, userId);
    return ResponseEntity.noContent().build();
  }

  // Helper — extrair UUID do utilizador autenticado
  private UUID getUserId(UserDetails userDetails) {
    if (userDetails instanceof JwtUserDetails jwtUser) {
      return jwtUser.getUserId();
    }
    throw new AccessDeniedException("A sessão autenticada não identifica um utilizador.");
  }
}
