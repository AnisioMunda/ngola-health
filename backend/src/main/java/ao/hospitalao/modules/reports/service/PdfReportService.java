package ao.hospitalao.modules.reports.service;

import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.laboratory.entity.LabRequest;
import ao.hospitalao.modules.laboratory.repository.LabRequestRepository;
import ao.hospitalao.modules.patients.entity.Patient;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.modules.pharmacy.entity.Medication;
import ao.hospitalao.modules.pharmacy.repository.MedicationRepository;
import ao.hospitalao.modules.pharmacy.repository.StockBatchRepository;
import ao.hospitalao.security.tenant.TenantContext;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfReportService {

    private final PatientRepository patientRepository;
    private final LabRequestRepository labRequestRepository;
    private final MedicationRepository medicationRepository;
    private final StockBatchRepository stockBatchRepository;
    private final HospitalRepository hospitalRepository;

    private static final DeviceRgb PRIMARY   = new DeviceRgb(32, 58, 67);
    private static final DeviceRgb ACCENT    = new DeviceRgb(94, 231, 223);
    private static final DeviceRgb LIGHT_BG  = new DeviceRgb(248, 249, 251);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ------------------------------------------------
    // Patient Report — ficha clínica
    // ------------------------------------------------

    @Transactional(readOnly = true)
    public byte[] generatePatientReport(UUID patientId) throws IOException {
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new EntityNotFoundException("Patient not found: " + patientId));

        Hospital hospital = getHospital();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(40, 40, 40, 40);

        PdfFont bold    = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

        // Header
        addHeader(doc, hospital, "PATIENT RECORD", bold, regular);

        // Patient info table
        doc.add(new Paragraph("PERSONAL INFORMATION")
            .setFont(bold).setFontSize(10).setFontColor(PRIMARY)
            .setMarginTop(16).setMarginBottom(6));

        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 2, 1, 2}))
            .useAllAvailableWidth();

        addRow(infoTable, bold, regular,
            "Full Name", patient.getFullName(),
            "Date of Birth", patient.getBirthDate() != null ? patient.getBirthDate().format(DATE_FMT) : "—");
        addRow(infoTable, bold, regular,
            "Gender", patient.getGender() != null ? patient.getGender().name() : "—",
            "Age", patient.getBirthDate() != null
                ? java.time.Period.between(patient.getBirthDate(), LocalDate.now()).getYears() + " years" : "—");
        addRow(infoTable, bold, regular,
            "National ID", nvl(patient.getNationalId()),
            "Health Card", nvl(patient.getHealthCardNumber()));
        addRow(infoTable, bold, regular,
            "Phone", nvl(patient.getPhone()),
            "Email", nvl(patient.getEmail()));
        addRow(infoTable, bold, regular,
            "Province", nvl(patient.getProvince()),
            "Municipality", nvl(patient.getMunicipality()));

        doc.add(infoTable);

        // Clinical info
        doc.add(new Paragraph("CLINICAL INFORMATION")
            .setFont(bold).setFontSize(10).setFontColor(PRIMARY)
            .setMarginTop(14).setMarginBottom(6));

        Table clinTable = new Table(UnitValue.createPercentArray(new float[]{1, 2, 1, 2}))
            .useAllAvailableWidth();

        addRow(clinTable, bold, regular,
            "Blood Type", nvl(patient.getBloodType()),
            "Allergies", nvl(patient.getAllergies()));
        addRow(clinTable, bold, regular,
            "Chronic Conditions", nvl(patient.getChronicConditions()),
            "Notes", nvl(patient.getNotes()));

        doc.add(clinTable);

        // Emergency contact
        if (patient.getEmergencyContactName() != null) {
            doc.add(new Paragraph("EMERGENCY CONTACT")
                .setFont(bold).setFontSize(10).setFontColor(PRIMARY)
                .setMarginTop(14).setMarginBottom(6));

            Table emgTable = new Table(UnitValue.createPercentArray(new float[]{1, 2, 1, 2}))
                .useAllAvailableWidth();
            addRow(emgTable, bold, regular,
                "Name", nvl(patient.getEmergencyContactName()),
                "Phone", nvl(patient.getEmergencyContactPhone()));
            addRow(emgTable, bold, regular,
                "Relationship", nvl(patient.getEmergencyContactRelationship()),
                "", "");
            doc.add(emgTable);
        }

        addFooter(doc, regular);
        doc.close();

        log.info("Patient report generated for: {}", patient.getFullName());
        return baos.toByteArray();
    }

    // ------------------------------------------------
    // Stock Report — inventário de medicamentos
    // ------------------------------------------------

    @Transactional(readOnly = true)
    public byte[] generateStockReport() throws IOException {
        UUID hospitalId = TenantContext.getCurrentHospital();
        Hospital hospital = getHospital();
        List<Medication> medications = medicationRepository.findByHospitalIdAndActiveTrue(hospitalId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(40, 40, 40, 40);

        PdfFont bold    = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

        addHeader(doc, hospital, "PHARMACY STOCK REPORT", bold, regular);

        doc.add(new Paragraph("Generated: " + OffsetDateTime.now().format(DT_FMT))
            .setFont(regular).setFontSize(9).setFontColor(ColorConstants.GRAY)
            .setMarginBottom(12));

        // Stock table
        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 1.5f, 1, 1.5f, 1.5f}))
            .useAllAvailableWidth();

        // Header row
        String[] headers = {"Medication", "Form", "Unit", "Total Stock", "Min Level"};
        for (String h : headers) {
            table.addHeaderCell(new Cell()
                .add(new Paragraph(h).setFont(bold).setFontSize(9))
                .setBackgroundColor(PRIMARY).setFontColor(ColorConstants.WHITE)
                .setPadding(6));
        }

        boolean alt = false;
        for (Medication med : medications) {
            int total = stockBatchRepository.getTotalAvailableQuantity(med.getId(), LocalDate.now());
            boolean low = total <= med.getMinStockLevel();
            DeviceRgb rowBg = alt ? LIGHT_BG : new DeviceRgb(255, 255, 255);

            table.addCell(new Cell().add(new Paragraph(med.getName()).setFont(bold).setFontSize(9)).setBackgroundColor(rowBg).setPadding(5));
            table.addCell(new Cell().add(new Paragraph(med.getDosageForm().name()).setFont(regular).setFontSize(9)).setBackgroundColor(rowBg).setPadding(5));
            table.addCell(new Cell().add(new Paragraph(med.getUnit()).setFont(regular).setFontSize(9)).setBackgroundColor(rowBg).setPadding(5));

            Paragraph stockPara = new Paragraph(String.valueOf(total)).setFont(bold).setFontSize(9);
            if (low) stockPara.setFontColor(new DeviceRgb(220, 38, 38));
            table.addCell(new Cell().add(stockPara).setBackgroundColor(rowBg).setPadding(5));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(med.getMinStockLevel())).setFont(regular).setFontSize(9)).setBackgroundColor(rowBg).setPadding(5));

            alt = !alt;
        }

        doc.add(table);

        if (medications.isEmpty()) {
            doc.add(new Paragraph("No medications in catalog.")
                .setFont(regular).setFontSize(10).setFontColor(ColorConstants.GRAY));
        }

        addFooter(doc, regular);
        doc.close();

        log.info("Stock report generated: {} medications", medications.size());
        return baos.toByteArray();
    }

    // ------------------------------------------------
    // Helpers
    // ------------------------------------------------

    private void addHeader(Document doc, Hospital hospital, String title,
                           PdfFont bold, PdfFont regular) throws IOException {
        // Banner
        Table header = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
            .useAllAvailableWidth()
            .setBackgroundColor(PRIMARY);

        // Left: hospital name
        String hospitalName = hospital != null ? hospital.getName() : "HospitalAO";
        header.addCell(new Cell()
            .add(new Paragraph(hospitalName)
                .setFont(bold).setFontSize(14).setFontColor(ColorConstants.WHITE))
            .add(new Paragraph(hospital != null ? hospital.getProvince() : "Angola")
                .setFont(regular).setFontSize(9).setFontColor(ACCENT))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
            .setPadding(12));

        // Right: report title
        header.addCell(new Cell()
            .add(new Paragraph(title)
                .setFont(bold).setFontSize(11).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
            .setPadding(12));

        doc.add(header);
    }

    private void addRow(Table table, PdfFont bold, PdfFont regular,
                        String k1, String v1, String k2, String v2) {
        table.addCell(new Cell().add(new Paragraph(k1).setFont(bold).setFontSize(9))
            .setBackgroundColor(LIGHT_BG).setPadding(5));
        table.addCell(new Cell().add(new Paragraph(v1).setFont(regular).setFontSize(9))
            .setPadding(5));
        table.addCell(new Cell().add(new Paragraph(k2).setFont(bold).setFontSize(9))
            .setBackgroundColor(LIGHT_BG).setPadding(5));
        table.addCell(new Cell().add(new Paragraph(v2).setFont(regular).setFontSize(9))
            .setPadding(5));
    }

    private void addFooter(Document doc, PdfFont regular) {
        doc.add(new Paragraph("\nGenerated by HospitalAO · " + LocalDate.now().format(DATE_FMT))
            .setFont(regular).setFontSize(8)
            .setFontColor(ColorConstants.GRAY)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(20));
    }

    private Hospital getHospital() {
        UUID hospitalId = TenantContext.getCurrentHospital();
        if (hospitalId == null) return null;
        return hospitalRepository.findById(hospitalId).orElse(null);
    }

    private String nvl(String s) { return s != null && !s.isBlank() ? s : "—"; }
}