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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfReportService {

    private static final Color PRIMARY = new Color(32, 58, 67);
    private static final Color ACCENT = new Color(94, 231, 223);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PatientRepository patientRepository;
    private final MedicationRepository medicationRepository;
    private final StockBatchRepository stockBatchRepository;
    private final HospitalRepository hospitalRepository;

    @Transactional(readOnly = true)
    public byte[] generatePatientReport(UUID patientId) throws IOException {
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new EntityNotFoundException("Patient not found: " + patientId));
        Hospital hospital = getHospital();

        try (PdfDocumentBuilder pdf = new PdfDocumentBuilder(40)) {
            pdf.addBanner(hospital != null ? hospital.getName() : "HospitalAO",
                hospital != null ? nvl(hospital.getProvince()) : "Angola",
                PRIMARY, ACCENT);
            pdf.addTitle("PATIENT RECORD", 13, PRIMARY);

            pdf.addSection("PERSONAL INFORMATION");
            pdf.addTable(new String[]{"Field", "Value", "Field", "Value"},
                rows(
                    "Full Name", patient.getFullName(),
                    "Date of Birth", patient.getBirthDate() != null
                        ? patient.getBirthDate().format(DATE_FMT) : "-",
                    "Gender", patient.getGender() != null ? patient.getGender().name() : "-",
                    "Age", patient.getBirthDate() != null
                        ? java.time.Period.between(patient.getBirthDate(), LocalDate.now()).getYears()
                            + " years" : "-",
                    "National ID", nvl(patient.getNationalId()),
                    "Health Card", nvl(patient.getHealthCardNumber()),
                    "Phone", nvl(patient.getPhone()),
                    "Email", nvl(patient.getEmail()),
                    "Province", nvl(patient.getProvince()),
                    "Municipality", nvl(patient.getMunicipality())
                ), new float[]{1, 2, 1, 2}, 8, new Color(235, 240, 242), Color.DARK_GRAY);

            pdf.addSection("CLINICAL INFORMATION");
            pdf.addTable(new String[]{"Field", "Value", "Field", "Value"},
                rows(
                    "Blood Type", nvl(patient.getBloodType()),
                    "Allergies", nvl(patient.getAllergies()),
                    "Chronic Conditions", nvl(patient.getChronicConditions()),
                    "Notes", nvl(patient.getNotes())
                ), new float[]{1, 2, 1, 2}, 8, new Color(235, 240, 242), Color.DARK_GRAY);

            if (patient.getEmergencyContactName() != null) {
                pdf.addSection("EMERGENCY CONTACT");
                pdf.addTable(new String[]{"Field", "Value", "Field", "Value"},
                    rows(
                        "Name", nvl(patient.getEmergencyContactName()),
                        "Phone", nvl(patient.getEmergencyContactPhone()),
                        "Relationship", nvl(patient.getEmergencyContactRelationship()),
                        "", ""
                    ), new float[]{1, 2, 1, 2}, 8, new Color(235, 240, 242), Color.DARK_GRAY);
            }

            addFooter(pdf);
            return pdf.toByteArray();
        } finally {
            log.info("Patient report generated for: {}", patient.getFullName());
        }
    }

    @Transactional(readOnly = true)
    public byte[] generateStockReport() throws IOException {
        UUID hospitalId = TenantContext.getCurrentHospital();
        Hospital hospital = getHospital();
        List<Medication> medications =
            medicationRepository.findByHospitalIdAndActiveTrue(hospitalId);

        try (PdfDocumentBuilder pdf = new PdfDocumentBuilder(40)) {
            pdf.addBanner(hospital != null ? hospital.getName() : "HospitalAO",
                hospital != null ? nvl(hospital.getProvince()) : "Angola",
                PRIMARY, ACCENT);
            pdf.addTitle("PHARMACY STOCK REPORT", 13, PRIMARY);
            pdf.addText("Generated: " + OffsetDateTime.now().format(DT_FMT),
                9, Color.GRAY, false, 4);

            List<String[]> rows = new ArrayList<>();
            for (Medication medication : medications) {
                int total = stockBatchRepository.getTotalAvailableQuantity(
                    medication.getId(), LocalDate.now());
                boolean low = total <= medication.getMinStockLevel();
                rows.add(new String[]{
                    medication.getName(),
                    medication.getDosageForm().name(),
                    medication.getUnit(),
                    String.valueOf(total) + (low ? " (LOW)" : ""),
                    String.valueOf(medication.getMinStockLevel())
                });
            }
            pdf.addTable(new String[]{"Medication", "Form", "Unit", "Total Stock", "Min Level"},
                rows, new float[]{3, 1.5f, 1, 1.5f, 1.5f}, 8,
                PRIMARY, Color.WHITE);

            if (medications.isEmpty()) {
                pdf.addText("No medications in catalog.", 10, Color.GRAY, false, 4);
            }

            addFooter(pdf);
            byte[] result = pdf.toByteArray();
            log.info("Stock report generated: {} medications", medications.size());
            return result;
        }
    }

    private void addFooter(PdfDocumentBuilder pdf) throws IOException {
        pdf.addText("Generated by HospitalAO - " + LocalDate.now().format(DATE_FMT),
            8, Color.GRAY, false, 4);
    }

    private List<String[]> rows(String... values) {
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < values.length; i += 4) {
            rows.add(new String[]{values[i], values[i + 1], values[i + 2], values[i + 3]});
        }
        return rows;
    }

    private Hospital getHospital() {
        UUID hospitalId = TenantContext.getCurrentHospital();
        if (hospitalId == null) return null;
        return hospitalRepository.findById(hospitalId).orElse(null);
    }

    private String nvl(String value) {
        return value != null && !value.isBlank() ? value : "-";
    }
}
