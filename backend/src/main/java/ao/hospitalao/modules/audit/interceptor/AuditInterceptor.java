package ao.hospitalao.modules.audit.interceptor;

import ao.hospitalao.modules.audit.entity.AuditLog.AuditAction;
import ao.hospitalao.modules.audit.entity.AuditLog.AuditResult;
import ao.hospitalao.modules.audit.entity.AuditLog.EntityType;
import ao.hospitalao.modules.audit.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor que regista automaticamente acções de escrita (POST/PATCH/DELETE) na tabela de
 * auditoria, sem necessidade de anotações em cada controller.
 *
 * <p>Leitura (GET) não é registada por omissão para não sobrecarregar a tabela. Apenas endpoints
 * sensíveis são registados manualmente via AuditService.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditInterceptor implements HandlerInterceptor {

  private final AuditService auditService;

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {

    String method = request.getMethod();

    // Apenas registar mutações
    if (!"POST".equals(method)
        && !"PATCH".equals(method)
        && !"PUT".equals(method)
        && !"DELETE".equals(method)) {
      return;
    }

    String url = request.getRequestURI();

    // Ignorar endpoints de autenticação (geridos separadamente no AuthService)
    if (url.contains("/auth/")) return;

    // Ignorar actuator e swagger
    if (url.contains("/actuator") || url.contains("/swagger") || url.contains("/v3/api-docs"))
      return;

    AuditAction action =
        switch (method) {
          case "POST" -> AuditAction.CREATE;
          case "PUT", "PATCH" -> AuditAction.UPDATE;
          case "DELETE" -> AuditAction.DELETE;
          default -> AuditAction.READ;
        };

    // Sobrescrever acções especiais baseado no URL
    if (url.contains("/issue")) action = AuditAction.APPROVE;
    if (url.contains("/void")) action = AuditAction.REJECT;
    if (url.contains("/discharge")) action = AuditAction.UPDATE;
    if (url.contains("/pdf")) action = AuditAction.PRINT;

    EntityType entityType = detectEntityType(url);
    String description = buildDescription(method, url, response.getStatus());

    AuditResult result =
        response.getStatus() < 400
            ? AuditResult.SUCCESS
            : response.getStatus() == 401 || response.getStatus() == 403
                ? AuditResult.UNAUTHORIZED
                : AuditResult.FAILURE;

    String ip = getClientIp(request);
    String ua = request.getHeader("User-Agent");
    String entityId = resolveEntityId(request, response);

    auditService.log(
        action,
        entityType,
        entityId,
        description,
        null,
        null,
        ip,
        ua,
        result,
        ex != null ? ex.getMessage() : null,
        method,
        url);
  }

  // ------------------------------------------------
  // Helpers
  // ------------------------------------------------

  private EntityType detectEntityType(String url) {
    if (url.contains("/patients")) return EntityType.PATIENT;
    if (url.contains("/episodes")) return EntityType.EPISODE;
    if (url.contains("/prescriptions")) return EntityType.PRESCRIPTION;
    if (url.contains("/lab")) return EntityType.LAB_REQUEST;
    if (url.contains("/pharmacy") || url.contains("/medications")) return EntityType.MEDICATION;
    if (url.contains("/financial") || url.contains("/invoices")) return EntityType.INVOICE;
    if (url.contains("/scheduling") || url.contains("/appointments")) return EntityType.APPOINTMENT;
    if (url.contains("/inpatient") || url.contains("/admissions")) return EntityType.ADMISSION;
    if (url.contains("/users")) return EntityType.USER;
    if (url.contains("/hospitals")) return EntityType.HOSPITAL;
    if (url.contains("/notifications")) return EntityType.NOTIFICATION;
    if (url.contains("/reports")) return EntityType.REPORT;
    if (url.contains("/wards") || url.contains("/beds")) return EntityType.WARD;
    return EntityType.SYSTEM;
  }

  private String resolveEntityId(HttpServletRequest request, HttpServletResponse response) {
    String location = response.getHeader("Location");
    if (location != null && !location.isBlank()) {
      String locationId = lastPathSegment(URI.create(location).getPath());
      if (locationId != null) return locationId;
    }

    String[] segments = request.getRequestURI().split("/");
    for (int i = segments.length - 1; i >= 0; i--) {
      String segment = segments[i];
      if (isUuid(segment)) return segment;
    }
    return null;
  }

  private String lastPathSegment(String path) {
    if (path == null || path.isBlank()) return null;
    String[] segments = path.split("/");
    String lastSegment = segments[segments.length - 1];
    return lastSegment.isBlank() ? null : lastSegment;
  }

  private boolean isUuid(String value) {
    if (value == null || value.isBlank()) return false;
    try {
      UUID.fromString(value);
      return true;
    } catch (IllegalArgumentException ignored) {
      return false;
    }
  }

  private String buildDescription(String method, String url, int status) {
    return String.format("%s %s → %d", method, url, status);
  }

  private String getClientIp(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip == null || ip.isBlank()) ip = request.getHeader("X-Real-IP");
    if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
    // Se há múltiplos IPs (proxy chain), pegar o primeiro
    if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
    return ip;
  }
}
