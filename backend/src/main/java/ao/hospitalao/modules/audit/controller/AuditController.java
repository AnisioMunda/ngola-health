package ao.hospitalao.modules.audit.controller;

import ao.hospitalao.modules.audit.entity.AuditLog.AuditAction;
import ao.hospitalao.modules.audit.entity.AuditLog.EntityType;
import ao.hospitalao.modules.audit.service.AuditService;
import ao.hospitalao.modules.audit.service.AuditService.AuditLogDto;
import ao.hospitalao.modules.audit.service.AuditService.AuditStatsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Logs de auditoria e rastreabilidade")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/logs")
    @Operation(summary = "Listar logs de auditoria com filtros")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Page<AuditLogDto>> findAll(
        @RequestParam(required = false) UUID userId,
        @RequestParam(required = false) AuditAction action,
        @RequestParam(required = false) EntityType entityType,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
        @PageableDefault(size = 30) Pageable pageable
    ) {
        return ResponseEntity.ok(
            auditService.findAll(userId, action, entityType, from, to, pageable));
    }

    @GetMapping("/logs/entity/{type}/{id}")
    @Operation(summary = "Histórico de auditoria de uma entidade específica")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<AuditLogDto>> findByEntity(
        @PathVariable EntityType type,
        @PathVariable String id
    ) {
        return ResponseEntity.ok(auditService.findByEntity(type, id));
    }

    @GetMapping("/stats")
    @Operation(summary = "Estatísticas de auditoria — dashboard de segurança")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<AuditStatsDto> getStats() {
        return ResponseEntity.ok(auditService.getStats());
    }
}