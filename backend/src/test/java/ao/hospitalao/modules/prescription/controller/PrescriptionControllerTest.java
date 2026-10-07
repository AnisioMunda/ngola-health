package ao.hospitalao.modules.prescription.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ao.hospitalao.config.SecurityConfig;
import ao.hospitalao.modules.audit.service.AuditService;
import ao.hospitalao.modules.auth.service.TokenBlackListService;
import ao.hospitalao.modules.prescription.dto.PrescriptionDtos.*;
import ao.hospitalao.modules.prescription.entity.Prescription.PrescriptionStatus;
import ao.hospitalao.modules.prescription.service.PrescriptionService;
import ao.hospitalao.security.RoleName;
import ao.hospitalao.security.UserDetailsServiceImpl;
import ao.hospitalao.security.jwt.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(controllers = PrescriptionController.class)
@Import(SecurityConfig.class)
@DisplayName("PrescriptionController — Testes de Integração")
class PrescriptionControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PrescriptionService prescriptionService;

  @MockitoBean private AuditService auditService;

  @MockitoBean private UserDetailsServiceImpl userDetailsService;

  @MockitoBean private JwtService jwtService;

  @MockitoBean private ObjectMapper jwtObjectMapper;

  @MockitoBean private TokenBlackListService blacklistService;

  private ObjectMapper objectMapper;
  private UUID prescriptionId;
  private PrescriptionResponse sampleResponse;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    prescriptionId = UUID.randomUUID();
    sampleResponse = buildSampleResponse();
  }

  // ------------------------------------------------
  // GET /prescriptions/stats
  // ------------------------------------------------

  @Test
  @DisplayName("GET /stats — deve retornar estatísticas")
  void shouldReturnStats() throws Exception {
    var stats =
        PrescriptionStatsDto.builder()
            .totalActive(5L)
            .totalToday(2L)
            .pendingDispense(3L)
            .expiringSoon(1L)
            .build();

    when(prescriptionService.getStats()).thenReturn(stats);

    mockMvc
        .perform(api(get("/api/prescriptions/stats"), "doctor", "DOCTOR"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalActive").value(5))
        .andExpect(jsonPath("$.totalToday").value(2))
        .andExpect(jsonPath("$.pendingDispense").value(3));
  }

  // ------------------------------------------------
  // GET /prescriptions
  // ------------------------------------------------

  @Test
  @DisplayName("GET / — deve listar prescrições paginadas")
  void shouldListPrescriptions() throws Exception {
    var page = new PageImpl<>(List.of(sampleResponse), PageRequest.of(0, 20), 1);

    when(prescriptionService.findAll(any(), any(), any())).thenReturn(page);

    mockMvc
        .perform(api(get("/api/prescriptions"), "pharmacist", "PHARMACIST"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].prescriptionNumber").value("RX-2026-00001"))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  // ------------------------------------------------
  // GET /prescriptions/{id}
  // ------------------------------------------------

  @Test
  @DisplayName("GET /{id} — deve retornar prescrição por ID")
  void shouldReturnPrescriptionById() throws Exception {
    when(prescriptionService.findById(prescriptionId)).thenReturn(sampleResponse);

    mockMvc
        .perform(api(get("/api/prescriptions/{id}", prescriptionId), "doctor", "DOCTOR"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(prescriptionId.toString()))
        .andExpect(jsonPath("$.patientName").value("João Silva"))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  @DisplayName("GET /episode/{episodeId} — deve retornar prescrições do episódio")
  void shouldReturnPrescriptionsByEpisode() throws Exception {
    UUID episodeId = UUID.randomUUID();
    when(prescriptionService.findByEpisode(episodeId)).thenReturn(List.of(sampleResponse));

    mockMvc
        .perform(api(get("/api/prescriptions/episode/{episodeId}", episodeId), "doctor", "DOCTOR"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].prescriptionNumber").value("RX-2026-00001"));
  }

  // ------------------------------------------------
  // POST /prescriptions
  // ------------------------------------------------

  @Test
  @DisplayName("POST / — deve criar prescrição")
  void shouldCreatePrescription() throws Exception {
    var itemReq = new CreatePrescriptionItemRequest();
    itemReq.setMedicationId(UUID.randomUUID());
    itemReq.setQuantityPrescribed(10);
    itemReq.setDosage("1 comprimido 3x ao dia");

    var req = new CreatePrescriptionRequest();
    req.setPatientId(UUID.randomUUID());
    req.setDiagnosis("Infecção bacteriana");
    req.setValidityDays(30);
    req.setItems(List.of(itemReq));

    when(prescriptionService.create(any())).thenReturn(sampleResponse);

    mockMvc
        .perform(
            api(post("/api/prescriptions"), "doctor", "DOCTOR")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.prescriptionNumber").value("RX-2026-00001"));
  }

  @Test
  @DisplayName("POST / — deve rejeitar acesso sem role adequada")
  void shouldRejectCreationWithoutProperRole() throws Exception {
    mockMvc
        .perform(
            api(post("/api/prescriptions"), "nurse", "NURSE")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @EnumSource(RoleName.class)
  @DisplayName("GET /stats — deve aplicar a matriz de perfis")
  void shouldEnforceStatsRoleMatrix(RoleName role) throws Exception {
    Set<RoleName> allowedRoles =
        Set.of(RoleName.ADMIN, RoleName.MANAGER, RoleName.DOCTOR, RoleName.PHARMACIST);
    int expectedStatus = allowedRoles.contains(role) ? 200 : 403;
    when(prescriptionService.getStats()).thenReturn(PrescriptionStatsDto.builder().build());

    mockMvc
        .perform(api(get("/api/prescriptions/stats"), role.name().toLowerCase(), role.name()))
        .andExpect(status().is(expectedStatus));
  }

  // ------------------------------------------------
  // POST /prescriptions/{id}/dispense
  // ------------------------------------------------

  @Test
  @DisplayName("POST /{id}/dispense — deve dispensar medicamento")
  void shouldDispenseMedication() throws Exception {
    var dispensed = buildSampleResponse();
    dispensed.setStatus(PrescriptionStatus.DISPENSED);

    when(prescriptionService.dispense(eq(prescriptionId), any())).thenReturn(dispensed);

    var req = new DispenseItemRequest();
    req.setPrescriptionItemId(UUID.randomUUID());
    req.setQuantityToDispense(5);

    mockMvc
        .perform(
            api(
                    post("/api/prescriptions/{id}/dispense", prescriptionId),
                    "pharmacist",
                    "PHARMACIST")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DISPENSED"));
  }

  // ------------------------------------------------
  // PATCH /prescriptions/{id}/cancel
  // ------------------------------------------------

  @Test
  @DisplayName("PATCH /{id}/cancel — deve cancelar prescrição")
  void shouldCancelPrescription() throws Exception {
    var cancelled = buildSampleResponse();
    cancelled.setStatus(PrescriptionStatus.CANCELLED);
    cancelled.setCancelledReason("Paciente com alergia");

    when(prescriptionService.cancel(eq(prescriptionId), any())).thenReturn(cancelled);

    var req = new CancelPrescriptionRequest();
    req.setReason("Paciente com alergia");

    mockMvc
        .perform(
            api(patch("/api/prescriptions/{id}/cancel", prescriptionId), "doctor", "DOCTOR")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"))
        .andExpect(jsonPath("$.cancelledReason").value("Paciente com alergia"));
  }

  // ------------------------------------------------
  // Segurança
  // ------------------------------------------------

  @Test
  @DisplayName("GET / — deve rejeitar acesso não autenticado")
  void shouldRejectUnauthenticatedAccess() throws Exception {
    mockMvc.perform(api(get("/api/prescriptions"))).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("GET / — deve rejeitar paciente em endpoint interno")
  void shouldRejectPatientRoleFromInternalEndpoint() throws Exception {
    mockMvc
        .perform(api(get("/api/prescriptions"), "patient", "PATIENT"))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("GET /portal/dashboard — deve rejeitar papel interno no portal")
  void shouldRejectInternalRoleFromPatientPortal() throws Exception {
    mockMvc
        .perform(api(get("/api/portal/dashboard"), "doctor", "DOCTOR"))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Actuator e Swagger — devem exigir papel administrativo")
  void shouldProtectActuatorAndApiDocumentation() throws Exception {
    mockMvc.perform(api(get("/api/actuator/health"))).andExpect(status().isUnauthorized());
    mockMvc
        .perform(api(get("/api/v3/api-docs"), "financial", "FINANCIAL"))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(api(get("/api/actuator/health"), "financial", "FINANCIAL"))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("CORS — deve aceitar apenas a origem configurada")
  void shouldAllowConfiguredCorsOrigin() throws Exception {
    mockMvc
        .perform(
            api(options("/api/prescriptions"))
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "authorization,content-type"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
  }

  @Test
  @DisplayName("CORS — deve rejeitar origens não configuradas")
  void shouldRejectUnconfiguredCorsOrigin() throws Exception {
    mockMvc
        .perform(
            api(options("/api/prescriptions"))
                .header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isForbidden());
  }

  // ------------------------------------------------
  // Builder
  // ------------------------------------------------

  private MockHttpServletRequestBuilder api(MockHttpServletRequestBuilder request) {
    return request.contextPath("/api");
  }

  private MockHttpServletRequestBuilder api(
      MockHttpServletRequestBuilder request, String username, String... roles) {
    return api(request).with(user(username).roles(roles));
  }

  private PrescriptionResponse buildSampleResponse() {
    return PrescriptionResponse.builder()
        .id(prescriptionId)
        .prescriptionNumber("RX-2026-00001")
        .patientId(UUID.randomUUID())
        .patientName("João Silva")
        .doctorId(UUID.randomUUID())
        .doctorName("Dr. António Costa")
        .status(PrescriptionStatus.ACTIVE)
        .statusLabel("Activa")
        .prescriptionDate(LocalDate.now())
        .expiryDate(LocalDate.now().plusDays(30))
        .expired(false)
        .items(List.of())
        .dispensations(List.of())
        .createdAt(OffsetDateTime.now())
        .build();
  }
}
