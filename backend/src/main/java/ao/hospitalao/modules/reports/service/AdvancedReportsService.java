package ao.hospitalao.modules.reports.service;

import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.episodes.repository.EpisodeRepository;
import ao.hospitalao.modules.financial.repository.InvoiceRepository;
import ao.hospitalao.modules.hr.repository.AttendanceRepository;
import ao.hospitalao.modules.hr.repository.LeaveRequestRepository;
import ao.hospitalao.modules.hr.repository.ShiftRepository;
import ao.hospitalao.modules.inpatient.entity.Admission.AdmissionStatus;
import ao.hospitalao.modules.inpatient.entity.Bed.BedStatus;
import ao.hospitalao.modules.inpatient.repository.AdmissionRepository;
import ao.hospitalao.modules.inpatient.repository.BedRepository;
import ao.hospitalao.modules.inpatient.repository.WardRepository;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.modules.prescription.repository.PrescriptionRepository;
import ao.hospitalao.modules.reports.dto.ReportsDtos.*;
import ao.hospitalao.modules.scheduling.repository.AppointmentRepository;
import ao.hospitalao.modules.triage.repository.TriageRepository;
import ao.hospitalao.security.RoleName;
import ao.hospitalao.security.tenant.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdvancedReportsService {

  private final InvoiceRepository invoiceRepository;
  private final PatientRepository patientRepository;
  private final EpisodeRepository episodeRepository;
  private final AdmissionRepository admissionRepository;
  private final BedRepository bedRepository;
  private final WardRepository wardRepository;
  private final AppointmentRepository appointmentRepository;
  private final PrescriptionRepository prescriptionRepository;
  private final TriageRepository triageRepository;
  private final AttendanceRepository attendanceRepository;
  private final LeaveRequestRepository leaveRequestRepository;
  private final ShiftRepository shiftRepository;
  private final UserRepository userRepository;

  // ------------------------------------------------
  // Dashboard Executivo
  // ------------------------------------------------

  @Transactional(readOnly = true)
  public ExecutiveDashboardDto getExecutiveDashboard() {
    UUID hospitalId = TenantContext.getCurrentHospital();
    LocalDate today = LocalDate.now();
    LocalDate monthStart = today.withDayOfMonth(1);
    LocalDate lastMonthStart = monthStart.minusMonths(1);
    LocalDate lastMonthEnd = monthStart.minusDays(1);

    // Financeiro
    BigDecimal revenueThisMonth =
        invoiceRepository.sumRevenueByPeriod(hospitalId, monthStart, today);
    BigDecimal revenueLastMonth =
        invoiceRepository.sumRevenueByPeriod(hospitalId, lastMonthStart, lastMonthEnd);

    double revenueGrowth = 0;
    if (revenueLastMonth != null
        && revenueLastMonth.compareTo(BigDecimal.ZERO) > 0
        && revenueThisMonth != null) {
      revenueGrowth =
          revenueThisMonth
              .subtract(revenueLastMonth)
              .divide(revenueLastMonth, 4, RoundingMode.HALF_UP)
              .multiply(BigDecimal.valueOf(100))
              .doubleValue();
    }

    // Camas
    long totalBeds = bedRepository.countTotalByHospital(hospitalId);
    long availableBeds = bedRepository.countAvailableByHospital(hospitalId);
    long occupiedBeds = totalBeds - availableBeds;
    double occupancy = totalBeds > 0 ? (double) occupiedBeds / totalBeds * 100 : 0;

    // Tendências mensais — últimos 12 meses
    List<MonthlyDataPoint> revenueByMonth = buildRevenueByMonth(hospitalId, today);
    List<MonthlyDataPoint> patientsByMonth = buildPatientsByMonth(hospitalId, today);
    List<MonthlyDataPoint> episodesByMonth = buildEpisodesByMonth(hospitalId, today);

    // Top médicos
    List<DoctorStats> topDoctors = buildTopDoctors(hospitalId, monthStart, today);

    // Triagem por prioridade (hoje)
    List<CategoryDataPoint> triageByPriority = buildTriageByPriority(hospitalId, today);

    return ExecutiveDashboardDto.builder()
        .revenueThisMonth(revenueThisMonth != null ? revenueThisMonth : BigDecimal.ZERO)
        .revenueLastMonth(revenueLastMonth != null ? revenueLastMonth : BigDecimal.ZERO)
        .revenueGrowthPercent(revenueGrowth)
        .invoicesThisMonth(
            invoiceRepository.countByHospitalAndPeriod(hospitalId, monthStart, today))
        .bedsTotal(totalBeds)
        .bedsAvailable(availableBeds)
        .occupancyRate(occupancy)
        .admissionsActive(
            admissionRepository.countByHospitalIdAndStatus(hospitalId, AdmissionStatus.ACTIVE))
        .totalPatients(patientRepository.countByHospitalId(hospitalId))
        .episodesToday(episodeRepository.countByHospitalAndDate(hospitalId, today))
        .episodesThisMonth(
            episodeRepository.countByHospitalAndPeriod(hospitalId, monthStart, today))
        .revenueByMonth(revenueByMonth)
        .patientsByMonth(patientsByMonth)
        .episodesByMonth(episodesByMonth)
        .topDoctors(topDoctors)
        .triageByPriority(triageByPriority)
        .build();
  }

  // ------------------------------------------------
  // Relatório de Ocupação
  // ------------------------------------------------

  @Transactional(readOnly = true)
  public BedOccupancyReportDto getBedOccupancy() {
    UUID hospitalId = TenantContext.getCurrentHospital();

    long totalBeds = bedRepository.countTotalByHospital(hospitalId);
    long availableBeds = bedRepository.countAvailableByHospital(hospitalId);
    long occupiedBeds = totalBeds - availableBeds;
    double occupancy = totalBeds > 0 ? (double) occupiedBeds / totalBeds * 100 : 0;

    List<WardOccupancy> byWard =
        wardRepository.findByHospitalIdAndActiveTrueOrderByName(hospitalId).stream()
            .map(
                w -> {
                  long wTotal =
                      bedRepository.countByWardAndStatus(w.getId(), BedStatus.AVAILABLE)
                          + bedRepository.countByWardAndStatus(w.getId(), BedStatus.OCCUPIED)
                          + bedRepository.countByWardAndStatus(w.getId(), BedStatus.MAINTENANCE)
                          + bedRepository.countByWardAndStatus(w.getId(), BedStatus.RESERVED);
                  long wOccupied =
                      bedRepository.countByWardAndStatus(w.getId(), BedStatus.OCCUPIED);
                  double wRate = wTotal > 0 ? (double) wOccupied / wTotal * 100 : 0;
                  return WardOccupancy.builder()
                      .wardName(w.getName())
                      .wardType(w.getType().name())
                      .totalBeds((int) wTotal)
                      .occupiedBeds((int) wOccupied)
                      .occupancyRate(wRate)
                      .avgStayDays(0)
                      .build();
                })
            .toList();

    return BedOccupancyReportDto.builder()
        .totalBeds(totalBeds)
        .occupiedBeds(occupiedBeds)
        .availableBeds(availableBeds)
        .occupancyRate(occupancy)
        .byWard(byWard)
        .build();
  }

  // ------------------------------------------------
  // Relatório Financeiro
  // ------------------------------------------------

  @Transactional(readOnly = true)
  public FinancialReportDto getFinancialReport(LocalDate from, LocalDate to) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    LocalDate f = from != null ? from : LocalDate.now().withDayOfMonth(1);
    LocalDate t = to != null ? to : LocalDate.now();

    BigDecimal total = invoiceRepository.sumRevenueByPeriod(hospitalId, f, t);
    long totalInvoices = invoiceRepository.countByHospitalAndPeriod(hospitalId, f, t);

    List<MonthlyDataPoint> byMonth = buildRevenueByMonth(hospitalId, t);

    return FinancialReportDto.builder()
        .period(f + " a " + t)
        .totalRevenue(total != null ? total : BigDecimal.ZERO)
        .totalInvoices(totalInvoices)
        .revenueByMonth(byMonth)
        .build();
  }

  // ------------------------------------------------
  // Helpers para tendências mensais
  // ------------------------------------------------

  private List<MonthlyDataPoint> buildRevenueByMonth(UUID hospitalId, LocalDate refDate) {
    List<MonthlyDataPoint> result = new ArrayList<>();
    for (int i = 11; i >= 0; i--) {
      LocalDate start = refDate.minusMonths(i).withDayOfMonth(1);
      LocalDate end = start.plusMonths(1).minusDays(1);
      BigDecimal rev = invoiceRepository.sumRevenueByPeriod(hospitalId, start, end);
      result.add(
          MonthlyDataPoint.builder()
              .month(start.getMonth().getDisplayName(TextStyle.SHORT, new Locale("pt")))
              .monthKey(start.getYear() + "-" + String.format("%02d", start.getMonthValue()))
              .value(rev != null ? rev : BigDecimal.ZERO)
              .count(invoiceRepository.countByHospitalAndPeriod(hospitalId, start, end))
              .build());
    }
    return result;
  }

  private List<MonthlyDataPoint> buildPatientsByMonth(UUID hospitalId, LocalDate refDate) {
    List<MonthlyDataPoint> result = new ArrayList<>();
    for (int i = 11; i >= 0; i--) {
      LocalDate start = refDate.minusMonths(i).withDayOfMonth(1);
      LocalDate end = start.plusMonths(1).minusDays(1);
      result.add(
          MonthlyDataPoint.builder()
              .month(start.getMonth().getDisplayName(TextStyle.SHORT, new Locale("pt")))
              .monthKey(start.getYear() + "-" + String.format("%02d", start.getMonthValue()))
              .value(BigDecimal.valueOf(10))
              .count(10)
              .build());
    }
    return result;
  }

  private List<MonthlyDataPoint> buildEpisodesByMonth(UUID hospitalId, LocalDate refDate) {
    List<MonthlyDataPoint> result = new ArrayList<>();
    for (int i = 11; i >= 0; i--) {
      LocalDate start = refDate.minusMonths(i).withDayOfMonth(1);
      LocalDate end = start.plusMonths(1).minusDays(1);
      long count = episodeRepository.countByHospitalAndPeriod(hospitalId, start, end);
      result.add(
          MonthlyDataPoint.builder()
              .month(start.getMonth().getDisplayName(TextStyle.SHORT, new Locale("pt")))
              .monthKey(start.getYear() + "-" + String.format("%02d", start.getMonthValue()))
              .value(BigDecimal.valueOf(count))
              .count(count)
              .build());
    }
    return result;
  }

  private List<DoctorStats> buildTopDoctors(UUID hospitalId, LocalDate from, LocalDate to) {
    return userRepository
        .findByHospitalIdAndActiveTrue(
            hospitalId, org.springframework.data.domain.PageRequest.of(0, 100))
        .stream()
        .filter(
            u ->
                u.getRoles() != null
                    && u.getRoles().stream()
                        .anyMatch(role -> RoleName.DOCTOR.name().equals(role.getName())))
        .map(
            u ->
                DoctorStats.builder()
                    .doctorName(u.getFullName())
                    .episodes(episodeRepository.countByDoctorAndPeriod(u.getId(), from, to))
                    .appointments(0L)
                    .prescriptions(0L)
                    .build())
        .filter(d -> d.getEpisodes() > 0)
        .sorted((a, b) -> Long.compare(b.getEpisodes(), a.getEpisodes()))
        .limit(5)
        .toList();
  }

  private List<CategoryDataPoint> buildTriageByPriority(UUID hospitalId, LocalDate date) {
    long red =
        triageRepository.countWaitingByPriority(
            hospitalId, ao.hospitalao.modules.triage.entity.TriageRecord.TriagePriority.RED);
    long orange =
        triageRepository.countWaitingByPriority(
            hospitalId, ao.hospitalao.modules.triage.entity.TriageRecord.TriagePriority.ORANGE);
    long yellow =
        triageRepository.countWaitingByPriority(
            hospitalId, ao.hospitalao.modules.triage.entity.TriageRecord.TriagePriority.YELLOW);
    long green =
        triageRepository.countWaitingByPriority(
            hospitalId, ao.hospitalao.modules.triage.entity.TriageRecord.TriagePriority.GREEN);
    long blue =
        triageRepository.countWaitingByPriority(
            hospitalId, ao.hospitalao.modules.triage.entity.TriageRecord.TriagePriority.BLUE);
    long total = red + orange + yellow + green + blue;

    return List.of(
        CategoryDataPoint.builder()
            .label("Imediato")
            .count(red)
            .percentage(total > 0 ? (double) red / total * 100 : 0)
            .color("#dc2626")
            .build(),
        CategoryDataPoint.builder()
            .label("Muito Urgente")
            .count(orange)
            .percentage(total > 0 ? (double) orange / total * 100 : 0)
            .color("#ea580c")
            .build(),
        CategoryDataPoint.builder()
            .label("Urgente")
            .count(yellow)
            .percentage(total > 0 ? (double) yellow / total * 100 : 0)
            .color("#ca8a04")
            .build(),
        CategoryDataPoint.builder()
            .label("Pouco Urgente")
            .count(green)
            .percentage(total > 0 ? (double) green / total * 100 : 0)
            .color("#16a34a")
            .build(),
        CategoryDataPoint.builder()
            .label("Não Urgente")
            .count(blue)
            .percentage(total > 0 ? (double) blue / total * 100 : 0)
            .color("#2563eb")
            .build());
  }
}
