package ao.hospitalao.modules.reports.service;

import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.patients.entity.Patient;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.modules.pharmacy.entity.Medication;
import ao.hospitalao.modules.pharmacy.repository.MedicationRepository;
import ao.hospitalao.modules.pharmacy.repository.StockBatchRepository;
import ao.hospitalao.security.tenant.TenantContext;
import ao.hospitalao.shared.pdf.PdfDocumentBuilder;
import jakarta.persistence.EntityNotFoundException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfReportService {

  private static final Color PRIMARY = new Color(32, 58, 67);
  private static final Color ACCENT = new Color(94, 231, 223);
  private static final ZoneId ANGOLA_ZONE = ZoneId.of("Africa/Luanda");
  private static final BufferedImage BRAND_LOGO = createBrandLogo();
  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private final PatientRepository patientRepository;
  private final MedicationRepository medicationRepository;
  private final StockBatchRepository stockBatchRepository;
  private final HospitalRepository hospitalRepository;

  @Transactional(readOnly = true)
  public byte[] generatePatientReport(UUID patientId) throws IOException {
    UUID hospitalId = TenantContext.getCurrentHospital();
    if (hospitalId == null || TenantContext.hasPlatformAccess()) {
      throw new AccessDeniedException("Patient reports require a hospital scope.");
    }
    Patient patient =
        patientRepository
            .findByHospitalIdAndId(hospitalId, patientId)
            .orElseThrow(() -> new EntityNotFoundException("Patient not found: " + patientId));
    Hospital hospital =
        hospitalRepository
            .findById(hospitalId)
            .orElseThrow(() -> new EntityNotFoundException("Hospital not found: " + hospitalId));

    try (PdfDocumentBuilder pdf = new PdfDocumentBuilder(40)) {
      pdf.addBrandBanner(hospital.getName(), hospital.getProvince(), PRIMARY, ACCENT, BRAND_LOGO);
      pdf.addTitle("FICHA DO PACIENTE", 13, PRIMARY);

      pdf.addSection("DADOS PESSOAIS");
      pdf.addTable(
          new String[] {"Campo", "Valor", "Campo", "Valor"},
          rows(
              "Nome completo", patient.getFullName(),
              "Data de nascimento",
                  patient.getBirthDate() != null ? patient.getBirthDate().format(DATE_FMT) : "-",
              "Género", genderLabel(patient.getGender()),
              "Idade",
                  patient.getBirthDate() != null
                      ? java.time.Period.between(patient.getBirthDate(), LocalDate.now(ANGOLA_ZONE))
                              .getYears()
                          + " anos"
                      : "-",
              "BI/NIF", nvl(patient.getNationalId()),
              "Cartão de saúde", nvl(patient.getHealthCardNumber()),
              "Telefone", nvl(patient.getPhone()),
              "Correio electrónico", nvl(patient.getEmail()),
              "Morada", nvl(patient.getAddress()),
              "Província", nvl(patient.getProvince()),
              "Município", nvl(patient.getMunicipality()),
              "Estado", patient.isActive() ? "Activo" : "Inactivo"),
          new float[] {1, 2, 1, 2},
          8,
          new Color(235, 240, 242),
          Color.DARK_GRAY);

      pdf.addSection("INFORMAÇÃO CLÍNICA");
      pdf.addTable(
          new String[] {"Campo", "Valor", "Campo", "Valor"},
          rows(
              "Grupo sanguíneo", nvl(patient.getBloodType()),
              "Alergias", nvl(patient.getAllergies()),
              "Doenças crónicas", nvl(patient.getChronicConditions()),
              "Observações", nvl(patient.getNotes())),
          new float[] {1, 2, 1, 2},
          8,
          new Color(235, 240, 242),
          Color.DARK_GRAY);

      if (hasEmergencyContact(patient)) {
        pdf.addSection("CONTACTO DE EMERGÊNCIA");
        pdf.addTable(
            new String[] {"Campo", "Valor", "Campo", "Valor"},
            rows(
                "Nome", nvl(patient.getEmergencyContactName()),
                "Telefone", nvl(patient.getEmergencyContactPhone()),
                "Parentesco", nvl(patient.getEmergencyContactRelationship()),
                "", ""),
            new float[] {1, 2, 1, 2},
            8,
            new Color(235, 240, 242),
            Color.DARK_GRAY);
      }

      addFooter(pdf);
      byte[] result = pdf.toByteArray();
      log.info("Patient report generated for: {}", patient.getId());
      return result;
    }
  }

  @Transactional(readOnly = true)
  public byte[] generateStockReport() throws IOException {
    UUID hospitalId = TenantContext.getCurrentHospital();
    if (hospitalId == null || TenantContext.hasPlatformAccess()) {
      throw new AccessDeniedException("Stock reports require a hospital scope.");
    }
    Hospital hospital =
        hospitalRepository
            .findById(hospitalId)
            .orElseThrow(() -> new EntityNotFoundException("Hospital not found: " + hospitalId));
    List<Medication> medications =
        medicationRepository.findByHospitalIdAndActiveTrueOrderByNameAsc(hospitalId);
    LocalDate reportDate = LocalDate.now(ANGOLA_ZONE);

    try (PdfDocumentBuilder pdf = new PdfDocumentBuilder(40)) {
      pdf.addBanner(hospital.getName(), nvl(hospital.getProvince()), PRIMARY, ACCENT);
      pdf.addTitle("INVENTÁRIO DE STOCK DA FARMÁCIA", 13, PRIMARY);
      pdf.addText(
          "Gerado em: " + OffsetDateTime.now(ANGOLA_ZONE).format(DT_FMT), 9, Color.GRAY, false, 4);

      List<String[]> rows = new ArrayList<>();
      for (Medication medication : medications) {
        int total = stockBatchRepository.getTotalAvailableQuantity(medication.getId(), reportDate);
        Integer minimum = medication.getMinStockLevel();
        boolean low = minimum != null && total <= minimum;
        rows.add(
            new String[] {
              medication.getName(),
              dosageFormLabel(medication.getDosageForm().name()),
              medication.getUnit(),
              String.valueOf(total),
              minimum != null ? String.valueOf(minimum) : "-",
              low ? "Stock baixo" : "Normal"
            });
      }
      pdf.addTable(
          new String[] {
            "Medicamento", "Forma", "Unidade", "Stock disponível", "Stock mínimo", "Estado"
          },
          rows,
          new float[] {2.3f, 1.1f, 1, 1.3f, 1.2f, 1.1f},
          7,
          PRIMARY,
          Color.WHITE);

      if (medications.isEmpty()) {
        pdf.addText("Não existem medicamentos activos no catálogo.", 10, Color.GRAY, false, 4);
      }

      addFooter(pdf);
      byte[] result = pdf.toByteArray();
      log.info("Stock inventory PDF generated: {} medications", medications.size());
      return result;
    }
  }

  private void addFooter(PdfDocumentBuilder pdf) throws IOException {
    pdf.addText(
        "Gerado pelo Ngola Health - " + LocalDate.now(ANGOLA_ZONE).format(DATE_FMT),
        8,
        Color.GRAY,
        false,
        4);
  }

  private String dosageFormLabel(String dosageForm) {
    return switch (dosageForm) {
      case "TABLET" -> "Comprimido";
      case "CAPSULE" -> "Cápsula";
      case "SYRUP" -> "Xarope";
      case "INJECTION" -> "Injectável";
      case "CREAM" -> "Creme";
      case "OINTMENT" -> "Pomada";
      case "DROPS" -> "Gotas";
      case "INHALER" -> "Inalador";
      case "OTHER" -> "Outro";
      default -> dosageForm;
    };
  }

  private String genderLabel(Patient.Gender gender) {
    if (gender == null) return "-";
    return gender == Patient.Gender.MALE ? "Masculino" : "Feminino";
  }

  private boolean hasEmergencyContact(Patient patient) {
    return (patient.getEmergencyContactName() != null
            && !patient.getEmergencyContactName().isBlank())
        || (patient.getEmergencyContactPhone() != null
            && !patient.getEmergencyContactPhone().isBlank())
        || (patient.getEmergencyContactRelationship() != null
            && !patient.getEmergencyContactRelationship().isBlank());
  }

  private static BufferedImage createBrandLogo() {
    BufferedImage image = new BufferedImage(120, 120, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = image.createGraphics();
    try {
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      graphics.setColor(new Color(22, 135, 126));
      graphics.fill(new RoundRectangle2D.Float(0, 0, 120, 120, 39, 39));
      graphics.setColor(Color.WHITE);
      graphics.setStroke(new BasicStroke(12, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      graphics.drawLine(60, 30, 60, 90);
      graphics.drawLine(30, 60, 90, 60);
    } finally {
      graphics.dispose();
    }
    return image;
  }

  private List<String[]> rows(String... values) {
    List<String[]> rows = new ArrayList<>();
    for (int i = 0; i < values.length; i += 4) {
      rows.add(new String[] {values[i], values[i + 1], values[i + 2], values[i + 3]});
    }
    return rows;
  }

  private String nvl(String value) {
    return value != null && !value.isBlank() ? value : "-";
  }
}
