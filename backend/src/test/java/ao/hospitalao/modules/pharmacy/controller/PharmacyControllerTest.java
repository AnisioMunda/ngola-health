package ao.hospitalao.modules.pharmacy.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ao.hospitalao.config.SecurityConfig;
import ao.hospitalao.modules.audit.service.AuditService;
import ao.hospitalao.modules.auth.service.TokenBlackListService;
import ao.hospitalao.modules.pharmacy.dto.PharmacyDtos.DispenseResponse;
import ao.hospitalao.modules.pharmacy.service.PharmacyService;
import ao.hospitalao.security.UserDetailsServiceImpl;
import ao.hospitalao.security.jwt.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(controllers = PharmacyController.class)
@Import(SecurityConfig.class)
class PharmacyControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PharmacyService pharmacyService;
  @MockitoBean private AuditService auditService;
  @MockitoBean private UserDetailsServiceImpl userDetailsService;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private ObjectMapper jwtObjectMapper;
  @MockitoBean private TokenBlackListService blacklistService;

  @Test
  void deniesMedicationDispensingToDoctors() throws Exception {
    mockMvc
        .perform(
            api(post("/api/pharmacy/dispense"), "doctor", "DOCTOR")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {"medicationId":"%s","quantity":1}
                        """
                        .formatted(UUID.randomUUID())))
        .andExpect(status().isForbidden());

    verifyNoInteractions(pharmacyService);
  }

  @Test
  void allowsPharmacistsToDispenseStock() throws Exception {
    UUID medicationId = UUID.randomUUID();
    when(pharmacyService.dispense(any()))
        .thenReturn(
            DispenseResponse.builder()
                .medicationId(medicationId)
                .medicationName("Medicamento de teste")
                .quantityDispensed(1)
                .remainingStock(4)
                .build());

    mockMvc
        .perform(
            api(post("/api/pharmacy/dispense"), "pharmacist", "PHARMACIST")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {"medicationId":"%s","quantity":1}
                        """
                        .formatted(medicationId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quantityDispensed").value(1))
        .andExpect(jsonPath("$.remainingStock").value(4));

    verify(pharmacyService).dispense(any());
  }

  @Test
  void keepsMedicationStockReceiptReadOnlyForManagers() throws Exception {
    mockMvc
        .perform(
            api(post("/api/pharmacy/stock/receive"), "manager", "MANAGER")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "medicationId":"%s",
                          "batchNumber":"LOTE-1",
                          "expiryDate":"2030-12-31",
                          "quantity":10
                        }
                        """
                        .formatted(UUID.randomUUID())))
        .andExpect(status().isForbidden());

    verifyNoInteractions(pharmacyService);
  }

  @Test
  void keepsMedicationCreationReadOnlyForManagers() throws Exception {
    mockMvc
        .perform(
            api(post("/api/pharmacy/medications"), "manager", "MANAGER")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "name":"Medicamento de teste",
                          "dosageForm":"TABLET",
                          "unit":"comprimido"
                        }
                        """))
        .andExpect(status().isForbidden());

    verifyNoInteractions(pharmacyService);
  }

  @Test
  void rejectsInvalidStockReceiptRequests() throws Exception {
    mockMvc
        .perform(
            api(post("/api/pharmacy/stock/receive"), "pharmacist", "PHARMACIST")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "medicationId":"%s",
                          "batchNumber":"LOTE-1",
                          "expiryDate":"2030-12-31",
                          "quantity":0
                        }
                        """
                        .formatted(UUID.randomUUID())))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(pharmacyService);
  }

  private MockHttpServletRequestBuilder api(
      MockHttpServletRequestBuilder request, String username, String... roles) {
    return request.contextPath("/api").with(user(username).roles(roles));
  }
}
