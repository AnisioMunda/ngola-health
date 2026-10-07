package ao.hospitalao.modules.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import ao.hospitalao.modules.audit.entity.AuditLog;
import ao.hospitalao.modules.audit.entity.AuditLog.AuditAction;
import ao.hospitalao.modules.audit.entity.AuditLog.AuditResult;
import ao.hospitalao.modules.audit.entity.AuditLog.EntityType;
import ao.hospitalao.modules.audit.repository.AuditLogRepository;
import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

  private static final String ENTITY_ID = UUID.randomUUID().toString();

  @Mock private AuditLogRepository auditLogRepository;

  @Mock private UserRepository userRepository;

  @Mock private HospitalRepository hospitalRepository;

  @InjectMocks private AuditService auditService;

  @BeforeEach
  void authenticateUser() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                "doctor", null, List.of(new SimpleGrantedAuthority("ROLE_DOCTOR"))));
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void persistsAuditEntryWithEntityAndRequestDetails() {
    auditService.log(
        AuditAction.UPDATE,
        EntityType.PRESCRIPTION,
        ENTITY_ID,
        "PATCH /api/prescriptions/" + ENTITY_ID,
        null,
        null,
        "192.0.2.10",
        "test-agent",
        AuditResult.SUCCESS,
        null,
        "PATCH",
        "/api/prescriptions/" + ENTITY_ID);

    ArgumentCaptor<AuditLog> auditLog = ArgumentCaptor.forClass(AuditLog.class);
    verify(auditLogRepository).save(auditLog.capture());
    assertThat(auditLog.getValue().getEntityId()).isEqualTo(ENTITY_ID);
    assertThat(auditLog.getValue().getUsername()).isEqualTo("doctor");
    assertThat(auditLog.getValue().getEntityType()).isEqualTo(EntityType.PRESCRIPTION);
    assertThat(auditLog.getValue().getHttpMethod()).isEqualTo("PATCH");
    assertThat(auditLog.getValue().getRequestUrl()).isEqualTo("/api/prescriptions/" + ENTITY_ID);
  }
}
