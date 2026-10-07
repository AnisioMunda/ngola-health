package ao.hospitalao.modules.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.audit.entity.AuditLog;
import ao.hospitalao.modules.audit.entity.AuditLog.AuditAction;
import ao.hospitalao.modules.audit.entity.AuditLog.AuditResult;
import ao.hospitalao.modules.audit.entity.AuditLog.EntityType;
import ao.hospitalao.modules.audit.repository.AuditLogRepository;
import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.security.tenant.TenantContext;
import java.time.OffsetDateTime;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
    TenantContext.clear();
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

  @Test
  void includesUserIdentifierInAuditLogResponse() {
    UUID userId = UUID.randomUUID();
    AuditLog log =
        AuditLog.builder()
            .id(UUID.randomUUID())
            .user(User.builder().id(userId).build())
            .action(AuditAction.LOGIN)
            .entityType(EntityType.USER)
            .entityId(userId.toString())
            .description("Login bem-sucedido")
            .result(AuditResult.SUCCESS)
            .createdAt(OffsetDateTime.now())
            .build();
    TenantContext.setPlatformAccess();
    when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(log)));

    var result = auditService.findAll(null, null, null, null, null, PageRequest.of(0, 30));

    assertThat(result.getContent()).singleElement().extracting("userId").isEqualTo(userId);
  }
}
