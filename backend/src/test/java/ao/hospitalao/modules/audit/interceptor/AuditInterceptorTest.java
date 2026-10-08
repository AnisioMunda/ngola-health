package ao.hospitalao.modules.audit.interceptor;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.audit.entity.AuditLog.AuditAction;
import ao.hospitalao.modules.audit.entity.AuditLog.AuditResult;
import ao.hospitalao.modules.audit.entity.AuditLog.EntityType;
import ao.hospitalao.modules.audit.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditInterceptorTest {

  @Mock private AuditService auditService;

  @InjectMocks private AuditInterceptor auditInterceptor;

  @Test
  void duplicateCheckPostIsAuditedAsReadRatherThanCreate() {
    var request = mock(HttpServletRequest.class);
    var response = mock(HttpServletResponse.class);
    String path = "/api/patients/possible-duplicates";

    when(request.getMethod()).thenReturn("POST");
    when(request.getRequestURI()).thenReturn(path);
    when(request.getHeader("X-Forwarded-For")).thenReturn(null);
    when(request.getHeader("X-Real-IP")).thenReturn(null);
    when(request.getHeader("User-Agent")).thenReturn(null);
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    when(response.getStatus()).thenReturn(HttpServletResponse.SC_OK);
    when(response.getHeader("Location")).thenReturn(null);

    auditInterceptor.afterCompletion(request, response, new Object(), null);

    verify(auditService)
        .log(
            eq(AuditAction.READ),
            eq(EntityType.PATIENT),
            isNull(),
            anyString(),
            isNull(),
            isNull(),
            eq("127.0.0.1"),
            isNull(),
            eq(AuditResult.SUCCESS),
            isNull(),
            eq("POST"),
            eq(path));
  }

  @Test
  void patientPortalAccountApprovalIsAuditedAsPatientApproval() {
    var request = mock(HttpServletRequest.class);
    var response = mock(HttpServletResponse.class);
    UUID accountId = UUID.randomUUID();
    String path = "/api/patient-portal/accounts/" + accountId + "/approve";

    when(request.getMethod()).thenReturn("PATCH");
    when(request.getRequestURI()).thenReturn(path);
    when(request.getHeader("X-Forwarded-For")).thenReturn(null);
    when(request.getHeader("X-Real-IP")).thenReturn(null);
    when(request.getHeader("User-Agent")).thenReturn(null);
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    when(response.getStatus()).thenReturn(HttpServletResponse.SC_NO_CONTENT);
    when(response.getHeader("Location")).thenReturn(null);

    auditInterceptor.afterCompletion(request, response, new Object(), null);

    verify(auditService)
        .log(
            eq(AuditAction.APPROVE),
            eq(EntityType.PATIENT),
            eq(accountId.toString()),
            anyString(),
            isNull(),
            isNull(),
            eq("127.0.0.1"),
            isNull(),
            eq(AuditResult.SUCCESS),
            isNull(),
            eq("PATCH"),
            eq(path));
  }
}
