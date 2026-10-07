package ao.hospitalao.modules.audit.service;

import ao.hospitalao.modules.audit.entity.AuditLog;
import ao.hospitalao.modules.audit.entity.AuditLog.*;
import ao.hospitalao.modules.audit.repository.AuditLogRepository;
import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.security.tenant.TenantContext;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

  private final AuditLogRepository auditLogRepository;
  private final UserRepository userRepository;
  private final HospitalRepository hospitalRepository;

  // ------------------------------------------------
  // Registar acção
  // ------------------------------------------------

  @Transactional
  public void log(AuditAction action, EntityType entityType, String entityId, String description) {
    log(
        action,
        entityType,
        entityId,
        description,
        null,
        null,
        null,
        null,
        AuditResult.SUCCESS,
        null,
        null,
        null);
  }

  @Transactional
  public void log(
      AuditAction action,
      EntityType entityType,
      String entityId,
      String description,
      String oldValues,
      String newValues,
      String ipAddress,
      String userAgent,
      AuditResult result,
      String errorMessage) {
    log(
        action,
        entityType,
        entityId,
        description,
        oldValues,
        newValues,
        ipAddress,
        userAgent,
        result,
        errorMessage,
        null,
        null);
  }

  @Transactional
  public void log(
      AuditAction action,
      EntityType entityType,
      String entityId,
      String description,
      String oldValues,
      String newValues,
      String ipAddress,
      String userAgent,
      AuditResult result,
      String errorMessage,
      String httpMethod,
      String requestUrl) {
    String username = null;
    String fullName = null;
    UUID userId = null;
    UUID hospitalId = TenantContext.getCurrentHospital();

    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
      username = auth.getName();

      if (auth.getPrincipal() instanceof ao.hospitalao.security.jwt.JwtUserDetails jwtUser) {
        userId = jwtUser.getUserId();
        fullName = userRepository.findById(userId).map(u -> u.getFullName()).orElse(username);
      }
    }

    AuditLog.AuditLogBuilder builder =
        AuditLog.builder()
            .action(action)
            .entityType(entityType)
            .entityId(entityId)
            .description(description)
            .username(username)
            .userFullName(fullName)
            .oldValues(oldValues)
            .newValues(newValues)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .httpMethod(httpMethod)
            .requestUrl(requestUrl)
            .result(result)
            .errorMessage(errorMessage);

    if (userId != null) builder.user(userRepository.getReferenceById(userId));
    if (hospitalId != null) builder.hospital(hospitalRepository.getReferenceById(hospitalId));

    auditLogRepository.save(builder.build());
  }

  // ------------------------------------------------
  // Atalhos para acções comuns
  // ------------------------------------------------

  public void logCreate(EntityType type, String id, String description) {
    log(AuditAction.CREATE, type, id, description);
  }

  public void logUpdate(
      EntityType type, String id, String description, String oldValues, String newValues) {
    log(
        AuditAction.UPDATE,
        type,
        id,
        description,
        oldValues,
        newValues,
        null,
        null,
        AuditResult.SUCCESS,
        null);
  }

  public void logDelete(EntityType type, String id, String description) {
    log(AuditAction.DELETE, type, id, description);
  }

  public void logRead(EntityType type, String id, String description) {
    log(AuditAction.READ, type, id, description);
  }

  public void logLogin(String username, String ip, boolean success) {
    AuditLog auditLog =
        AuditLog.builder()
            .action(success ? AuditAction.LOGIN : AuditAction.LOGIN_FAILED)
            .entityType(EntityType.USER)
            .entityId(username)
            .description(
                success
                    ? "Login bem-sucedido: " + username
                    : "Tentativa de login falhada: " + username)
            .username(username)
            .ipAddress(ip)
            .result(success ? AuditResult.SUCCESS : AuditResult.FAILURE)
            .build();
    auditLogRepository.save(auditLog);
  }

  public void logPrint(EntityType type, String id, String description) {
    log(AuditAction.PRINT, type, id, description);
  }

  // ------------------------------------------------
  // Queries para o painel de auditoria
  // ------------------------------------------------

  @Transactional(readOnly = true)
  public Page<AuditLogDto> findAll(
      UUID userId,
      AuditAction action,
      EntityType entityType,
      OffsetDateTime from,
      OffsetDateTime to,
      Pageable pageable) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    if (hospitalId == null && !TenantContext.hasPlatformAccess()) {
      return Page.empty(pageable);
    }
    return auditLogRepository
        .findAll(
            ao.hospitalao.modules.audit.repository.AuditSpecification.withFilters(
                hospitalId, userId, action, entityType, from, to),
            pageable)
        .map(this::toDto);
  }

  @Transactional(readOnly = true)
  public List<AuditLogDto> findByEntity(EntityType entityType, String entityId) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    if (hospitalId == null && !TenantContext.hasPlatformAccess()) {
      return List.of();
    }

    var logs =
        hospitalId == null
            ? auditLogRepository.findByEntity(entityType, entityId)
            : auditLogRepository.findByEntityAndHospital(entityType, entityId, hospitalId);
    return logs.stream().map(this::toDto).toList();
  }

  @Transactional(readOnly = true)
  public AuditStatsDto getStats() {
    UUID hospitalId = TenantContext.getCurrentHospital();
    OffsetDateTime since24h = OffsetDateTime.now().minusHours(24);
    OffsetDateTime since7d = OffsetDateTime.now().minusDays(7);

    long totalToday = auditLogRepository.countByHospitalSince(hospitalId, since24h);
    long failedLogins = auditLogRepository.countFailedLoginsSince(hospitalId, since7d);

    List<Map<String, Object>> topUsers =
        auditLogRepository.findTopActiveUsers(hospitalId, since7d).stream()
            .map(
                row ->
                    Map.of(
                        "username", row[0],
                        "fullName", row[1] != null ? row[1] : row[0],
                        "total", row[2]))
            .toList();

    return AuditStatsDto.builder()
        .totalToday(totalToday)
        .failedLoginsLast7Days(failedLogins)
        .topActiveUsers(topUsers)
        .build();
  }

  // ------------------------------------------------
  // DTOs
  // ------------------------------------------------

  private AuditLogDto toDto(AuditLog a) {
    return AuditLogDto.builder()
        .id(a.getId())
        .userId(a.getUser() != null ? a.getUser().getId() : null)
        .action(a.getAction())
        .entityType(a.getEntityType())
        .entityId(a.getEntityId())
        .description(a.getDescription())
        .username(a.getUsername())
        .userFullName(a.getUserFullName())
        .ipAddress(a.getIpAddress())
        .httpMethod(a.getHttpMethod())
        .requestUrl(a.getRequestUrl())
        .result(a.getResult())
        .errorMessage(a.getErrorMessage())
        .createdAt(a.getCreatedAt())
        .build();
  }

  @lombok.Data
  @lombok.Builder
  public static class AuditLogDto {
    private UUID id;
    private UUID userId;
    private AuditAction action;
    private EntityType entityType;
    private String entityId;
    private String description;
    private String username;
    private String userFullName;
    private String ipAddress;
    private String httpMethod;
    private String requestUrl;
    private AuditResult result;
    private String errorMessage;
    private OffsetDateTime createdAt;
  }

  @lombok.Data
  @lombok.Builder
  public static class AuditStatsDto {
    private long totalToday;
    private long failedLoginsLast7Days;
    private List<Map<String, Object>> topActiveUsers;
  }
}
