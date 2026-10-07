package ao.hospitalao.modules.hospitals.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ao.hospitalao.config.SecurityConfig;
import ao.hospitalao.modules.audit.service.AuditService;
import ao.hospitalao.modules.auth.service.TokenBlackListService;
import ao.hospitalao.modules.hospitals.dto.CreateHospitalRequest;
import ao.hospitalao.modules.hospitals.dto.HospitalResponse;
import ao.hospitalao.modules.hospitals.entity.Hospital.HospitalType;
import ao.hospitalao.modules.hospitals.service.HospitalService;
import ao.hospitalao.security.UserDetailsServiceImpl;
import ao.hospitalao.security.jwt.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(controllers = HospitalController.class)
@Import(SecurityConfig.class)
class HospitalControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private HospitalService hospitalService;
  @MockitoBean private AuditService auditService;
  @MockitoBean private UserDetailsServiceImpl userDetailsService;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private ObjectMapper jwtObjectMapper;
  @MockitoBean private TokenBlackListService blacklistService;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private UUID hospitalId;
  private HospitalResponse hospital;

  @BeforeEach
  void setUp() {
    hospitalId = UUID.randomUUID();
    hospital =
        HospitalResponse.builder()
            .id(hospitalId)
            .name("Hospital Central de Luanda")
            .code("HCL-001")
            .type(HospitalType.HOSPITAL)
            .province("Luanda")
            .active(true)
            .build();
  }

  @Test
  void hospitalAdminCannotCreateHospital() throws Exception {
    mockMvc
        .perform(
            api(post("/api/hospitals"), "hospital-admin", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(writeRequest()))
        .andExpect(status().isForbidden());

    verify(hospitalService, never()).create(any());
  }

  @Test
  void superAdminCanCreateHospital() throws Exception {
    when(hospitalService.create(any(CreateHospitalRequest.class))).thenReturn(hospital);

    mockMvc
        .perform(
            api(post("/api/hospitals"), "platform-admin", "SUPER_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(writeRequest()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(hospitalId.toString()));
  }

  @Test
  void onlySuperAdminCanListInactiveHospitals() throws Exception {
    mockMvc
        .perform(api(get("/api/hospitals/management"), "hospital-admin", "ADMIN"))
        .andExpect(status().isForbidden());

    verify(hospitalService, never()).findAll(true);
  }

  @Test
  void superAdminCanUpdateHospital() throws Exception {
    when(hospitalService.update(any(UUID.class), any(CreateHospitalRequest.class)))
        .thenReturn(hospital);

    mockMvc
        .perform(
            api(put("/api/hospitals/{id}", hospitalId), "platform-admin", "SUPER_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(writeRequest()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("HCL-001"));
  }

  @Test
  void deleteDeactivatesHospitalForSuperAdmin() throws Exception {
    when(hospitalService.setActive(hospitalId, false)).thenReturn(hospital);

    mockMvc
        .perform(api(delete("/api/hospitals/{id}", hospitalId), "platform-admin", "SUPER_ADMIN"))
        .andExpect(status().isNoContent());

    verify(hospitalService).setActive(hospitalId, false);
  }

  @Test
  void unauthenticatedUserCannotReadHospitals() throws Exception {
    mockMvc.perform(api(get("/api/hospitals"))).andExpect(status().isUnauthorized());
  }

  private String writeRequest() throws Exception {
    CreateHospitalRequest request = new CreateHospitalRequest();
    request.setName("Hospital Central de Luanda");
    request.setCode("HCL-001");
    request.setType(HospitalType.HOSPITAL);
    request.setProvince("Luanda");
    return objectMapper.writeValueAsString(request);
  }

  private MockHttpServletRequestBuilder api(MockHttpServletRequestBuilder request) {
    return request.contextPath("/api");
  }

  private MockHttpServletRequestBuilder api(
      MockHttpServletRequestBuilder request, String username, String... roles) {
    return api(request).with(user(username).roles(roles));
  }
}
