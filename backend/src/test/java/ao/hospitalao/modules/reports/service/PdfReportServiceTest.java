package ao.hospitalao.modules.reports.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.patients.entity.Patient;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.modules.pharmacy.repository.MedicationRepository;
import ao.hospitalao.modules.pharmacy.repository.StockBatchRepository;
import ao.hospitalao.security.tenant.TenantContext;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class PdfReportServiceTest {

  @Mock private PatientRepository patientRepository;
  @Mock private MedicationRepository medicationRepository;
  @Mock private StockBatchRepository stockBatchRepository;
  @Mock private HospitalRepository hospitalRepository;

  @InjectMocks private PdfReportService pdfReportService;

  @Test
  void patientReportPreservesPortugueseAccents() throws Exception {
    UUID patientId = UUID.randomUUID();
    UUID hospitalId = UUID.randomUUID();
    Patient patient =
        Patient.builder()
            .id(patientId)
            .fullName("João Gonçalves")
            .birthDate(LocalDate.of(1990, 4, 12))
            .gender(Patient.Gender.MALE)
            .province("Huíla")
            .emergencyContactName("Mário Gonçalves")
            .build();
    Hospital hospital =
        Hospital.builder().id(hospitalId).name("Hospital São João").province("Huíla").build();

    when(patientRepository.findByHospitalIdAndId(hospitalId, patientId))
        .thenReturn(Optional.of(patient));
    when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(hospital));

    byte[] pdf;
    try (MockedStatic<TenantContext> tenantContext = mockStatic(TenantContext.class)) {
      tenantContext.when(TenantContext::getCurrentHospital).thenReturn(hospitalId);
      tenantContext.when(TenantContext::hasPlatformAccess).thenReturn(false);
      pdf = pdfReportService.generatePatientReport(patientId);
    }

    try (var document = Loader.loadPDF(pdf)) {
      String text = new PDFTextStripper().getText(document);

      assertTrue(text.contains("Hospital São João"));
      assertTrue(text.contains("João Gonçalves"));
      assertTrue(text.contains("Huíla"));
      assertTrue(text.contains("Mário Gonçalves"));
      assertTrue(document.getNumberOfPages() > 0);
    }
  }

  @Test
  void patientReportRequiresHospitalScope() {
    try (MockedStatic<TenantContext> tenantContext = mockStatic(TenantContext.class)) {
      tenantContext.when(TenantContext::getCurrentHospital).thenReturn(null);
      tenantContext.when(TenantContext::hasPlatformAccess).thenReturn(false);

      assertThrows(
          AccessDeniedException.class,
          () -> pdfReportService.generatePatientReport(UUID.randomUUID()));
    }
  }

  @Test
  void stockReportCanBeGeneratedForAnEmptyCatalogue() throws Exception {
    UUID hospitalId = UUID.randomUUID();
    Hospital hospital = Hospital.builder().id(hospitalId).name("Hospital Geral").build();
    when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(hospital));
    when(medicationRepository.findByHospitalIdAndActiveTrue(hospitalId)).thenReturn(List.of());

    byte[] pdf;
    try (MockedStatic<TenantContext> tenantContext = mockStatic(TenantContext.class)) {
      tenantContext.when(TenantContext::getCurrentHospital).thenReturn(hospitalId);
      pdf = pdfReportService.generateStockReport();
    }

    try (var document = Loader.loadPDF(pdf)) {
      String text = new PDFTextStripper().getText(document);

      assertTrue(text.contains("PHARMACY STOCK REPORT"));
      assertTrue(text.contains("No medications in catalog."));
      assertTrue(document.getNumberOfPages() > 0);
    }
  }
}
