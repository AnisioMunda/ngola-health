package ao.hospitalao.modules.audit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ao.hospitalao.config.SecurityConfig;
import ao.hospitalao.modules.audit.entity.AuditLog.AuditAction;
import ao.hospitalao.modules.audit.entity.AuditLog.EntityType;
import ao.hospitalao.modules.audit.service.AuditService;
import ao.hospitalao.modules.auth.service.TokenBlackListService;
import ao.hospitalao.security.UserDetailsServiceImpl;
import ao.hospitalao.security.jwt.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(controllers = AuditController.class)
@Import(SecurityConfig.class)
class AuditControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AuditService auditService;

  @MockitoBean private UserDetailsServiceImpl userDetailsService;

  @MockitoBean private JwtService jwtService;

  @MockitoBean private ObjectMapper jwtObjectMapper;

  @MockitoBean private TokenBlackListService blacklistService;

  @Test
  void managerCanQueryAuditWithUserEntityAndTimeFilters() throws Exception {
    UUID userId = UUID.randomUUID();
    OffsetDateTime from = OffsetDateTime.parse("2026-06-22T08:00:00Z");
    OffsetDateTime to = OffsetDateTime.parse("2026-06-23T08:00:00Z");
    when(auditService.findAll(
            eq(userId),
            eq(AuditAction.UPDATE),
            eq(EntityType.PRESCRIPTION),
            eq(from),
            eq(to),
            any(Pageable.class)))
        .thenReturn(Page.empty(PageRequest.of(0, 30)));

    mockMvc
        .perform(
            api(
                get("/api/audit/logs")
                    .param("userId", userId.toString())
                    .param("action", "UPDATE")
                    .param("entityType", "PRESCRIPTION")
                    .param("from", from.toString())
                    .param("to", to.toString()),
                "manager",
                "MANAGER"))
        .andExpect(status().isOk());

    verify(auditService)
        .findAll(
            eq(userId),
            eq(AuditAction.UPDATE),
            eq(EntityType.PRESCRIPTION),
            eq(from),
            eq(to),
            any(Pageable.class));
  }

  @Test
  void nonAdminRoleCannotQueryAudit() throws Exception {
    mockMvc
        .perform(api(get("/api/audit/logs"), "doctor", "DOCTOR"))
        .andExpect(status().isForbidden());
  }

  @Test
  void unauthenticatedUserCannotQueryAudit() throws Exception {
    mockMvc.perform(api(get("/api/audit/logs"))).andExpect(status().isUnauthorized());
  }

  private MockHttpServletRequestBuilder api(MockHttpServletRequestBuilder request) {
    return request.contextPath("/api");
  }

  private MockHttpServletRequestBuilder api(
      MockHttpServletRequestBuilder request, String username, String role) {
    return api(request).with(user(username).roles(role));
  }
}
