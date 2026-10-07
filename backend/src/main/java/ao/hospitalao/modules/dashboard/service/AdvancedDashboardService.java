package ao.hospitalao.modules.dashboard.service;

import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.dashboard.dto.AdvancedDashboardResponse;
import ao.hospitalao.modules.dashboard.dto.AdvancedDashboardResponse.*;
import ao.hospitalao.modules.episodes.repository.EpisodeRepository;
import ao.hospitalao.modules.financial.entity.Invoice.InvoiceStatus;
import ao.hospitalao.modules.financial.repository.InvoiceRepository;
import ao.hospitalao.modules.laboratory.repository.LabRequestRepository;
import ao.hospitalao.modules.patients.entity.Patient;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.modules.pharmacy.repository.MedicationRepository;
import ao.hospitalao.modules.pharmacy.repository.StockBatchRepository;
import ao.hospitalao.security.tenant.TenantContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdvancedDashboardService {

  private final PatientRepository patientRepository;
  private final EpisodeRepository episodeRepository;
  private final InvoiceRepository invoiceRepository;
  private final LabRequestRepository labRequestRepository;
  private final MedicationRepository medicationRepository;
  private final StockBatchRepository stockBatchRepository;
  private final UserRepository userRepository;

  private static final DateTimeFormatter MONTH_LABEL =
      DateTimeFormatter.ofPattern("MMM", new Locale("pt", "AO"));

  @Transactional(readOnly = true)
  public AdvancedDashboardResponse getDashboard() {
    UUID hospitalId = TenantContext.getCurrentHospital();
    OffsetDateTime now = OffsetDateTime.now();
    OffsetDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
    OffsetDateTime lastMonthStart = monthStart.minusMonths(1);
    OffsetDateTime weekStart = now.minusDays(7);
    OffsetDateTime yearStart = now.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0);
    OffsetDateTime sixMonthsAgo = now.minusMonths(6);
    LocalDate today = LocalDate.now();

    // ------------------------------------------------
    // KPIs Clínicos
    // ------------------------------------------------
    long totalPatients = patientRepository.countByActiveTrue();
    long newThisMonth = patientRepository.countByActiveTrueAndCreatedAtAfter(monthStart);
    long newThisWeek = patientRepository.countByActiveTrueAndCreatedAtAfter(weekStart);
    long totalEpisodes = episodeRepository.count();
    long activeEpisodes =
        episodeRepository.countByStatus(
            ao.hospitalao.modules.episodes.entity.Episode.EpisodeStatus.IN_PROGRESS);
    long scheduledToday = episodeRepository.countScheduledToday(hospitalId, today);
    long completedToday = episodeRepository.countCompletedToday(hospitalId, today);
    long malePat = patientRepository.countByActiveTrueAndGender(Patient.Gender.MALE);
    long femalePat = patientRepository.countByActiveTrueAndGender(Patient.Gender.FEMALE);

    // ------------------------------------------------
    // KPIs Financeiros
    // ------------------------------------------------
    BigDecimal revenueThisMonth =
        safeSum(invoiceRepository.sumPaidBetween(hospitalId, monthStart, now));
    BigDecimal revenueLastMonth =
        safeSum(invoiceRepository.sumPaidBetween(hospitalId, lastMonthStart, monthStart));
    BigDecimal revenueThisYear =
        safeSum(invoiceRepository.sumPaidBetween(hospitalId, yearStart, now));
    BigDecimal pendingAmount = safeSum(invoiceRepository.sumPendingByHospital(hospitalId));
    long pendingInvoices = invoiceRepository.countPendingByHospital(hospitalId);

    long invoicesThisMonth =
        invoiceRepository.countByHospitalIdAndIssuedAtBetween(hospitalId, monthStart, now);
    long paidThisMonth =
        invoiceRepository.countByHospitalIdAndStatusAndPaidAtBetween(
            hospitalId, InvoiceStatus.PAGO, monthStart, now);

    double collectionRate =
        invoicesThisMonth > 0
            ? Math.round((double) paidThisMonth / invoicesThisMonth * 100.0)
            : 0.0;

    // ------------------------------------------------
    // KPIs Lab e Farmácia
    // ------------------------------------------------
    long labThisMonth =
        labRequestRepository.countByHospitalIdAndCreatedAtAfter(hospitalId, monthStart);
    long labPending =
        labRequestRepository.countByHospitalIdAndStatus(
            hospitalId, ao.hospitalao.modules.laboratory.entity.LabRequest.RequestStatus.PENDING);

    List<UUID> medicationIds =
        medicationRepository.findByHospitalIdAndActiveTrue(hospitalId).stream()
            .map(m -> m.getId())
            .toList();

    long lowStock =
        medicationIds.stream()
            .filter(
                id -> {
                  int avail =
                      Optional.ofNullable(stockBatchRepository.getTotalAvailableQuantity(id, today))
                          .orElse(0);
                  return avail <= 10;
                })
            .count();

    long expiring =
        stockBatchRepository.findExpiringSoon(hospitalId, today, today.plusDays(30)).size();

    // ------------------------------------------------
    // Tendências mensais (6 meses)
    // ------------------------------------------------
    List<MonthlyRevenue> revenueByMonth = buildRevenueByMonth(hospitalId, sixMonthsAgo);
    List<MonthlyPatients> patientsByMonth = buildPatientsByMonth(hospitalId, sixMonthsAgo);
    List<MonthlyEpisodes> episodesByMonth = buildEpisodesByMonth(hospitalId, sixMonthsAgo);

    // ------------------------------------------------
    // Distribuições
    // ------------------------------------------------
    Map<String, Long> episodesByType = new LinkedHashMap<>();
    episodeRepository
        .countByType(hospitalId)
        .forEach(p -> episodesByType.put(p.getType(), p.getCount()));

    Map<String, Long> usersByRole = new LinkedHashMap<>();
    userRepository.countByRole().forEach(r -> usersByRole.put(r.getRoleName(), r.getCount()));

    List<ProvinceCount> patientsByProvince =
        patientRepository.countByProvince(PageRequest.of(0, 5)).stream()
            .map(p -> ProvinceCount.builder().province(p.getProvince()).count(p.getCount()).build())
            .toList();

    // Documentos recentes
    List<RecentInvoice> recentInvoices =
        invoiceRepository.findTop5ByHospitalIdOrderByCreatedAtDesc(hospitalId).stream()
            .map(
                i ->
                    RecentInvoice.builder()
                        .id(i.getId().toString())
                        .invoiceNumber(i.getInvoiceNumber())
                        .patientName(i.getPatient().getFullName())
                        .totalAmount(i.getTotalAmount())
                        .status(i.getStatus().name())
                        .createdAt(i.getCreatedAt().toString())
                        .build())
            .toList();

    List<RecentPatient> recentPatients =
        patientRepository.findTop5ByActiveTrueOrderByCreatedAtDesc().stream()
            .map(
                p ->
                    RecentPatient.builder()
                        .id(p.getId().toString())
                        .fullName(p.getFullName())
                        .createdAt(p.getCreatedAt().toString())
                        .build())
            .toList();

    return AdvancedDashboardResponse.builder()
        // Clínicos
        .totalPatients(totalPatients)
        .newPatientsThisMonth(newThisMonth)
        .newPatientsThisWeek(newThisWeek)
        .totalEpisodes(totalEpisodes)
        .activeEpisodes(activeEpisodes)
        .scheduledToday(scheduledToday)
        .completedToday(completedToday)
        .malePatients(malePat)
        .femalePatients(femalePat)
        // Financeiros
        .revenueThisMonth(revenueThisMonth)
        .revenueLastMonth(revenueLastMonth)
        .revenueThisYear(revenueThisYear)
        .pendingAmount(pendingAmount)
        .invoicesThisMonth(invoicesThisMonth)
        .paidInvoicesThisMonth(paidThisMonth)
        .pendingInvoices(pendingInvoices)
        .collectionRate(collectionRate)
        // Lab / Farmácia
        .labRequestsThisMonth(labThisMonth)
        .labRequestsPending(labPending)
        .lowStockMedications(lowStock)
        .expiringMedications(expiring)
        // Tendências
        .revenueByMonth(revenueByMonth)
        .patientsByMonth(patientsByMonth)
        .episodesByMonth(episodesByMonth)
        // Distribuições
        .episodesByType(episodesByType)
        .usersByRole(usersByRole)
        .patientsByProvince(patientsByProvince)
        .recentInvoices(recentInvoices)
        .recentPatients(recentPatients)
        .build();
  }

  // ------------------------------------------------
  // Builders de tendências
  // ------------------------------------------------

  private List<MonthlyRevenue> buildRevenueByMonth(UUID hospitalId, OffsetDateTime since) {
    List<MonthlyRevenue> result = new ArrayList<>();
    List<Object[]> rows = invoiceRepository.revenueByMonthRaw(hospitalId, since);
    for (Object[] row : rows) {
      String ym = (String) row[0];
      BigDecimal revenue = toBD(row[1]);
      BigDecimal paid = toBD(row[2]);
      long count = toLong(row[3]);
      result.add(
          MonthlyRevenue.builder()
              .yearMonth(ym)
              .month(monthLabel(ym))
              .revenue(revenue)
              .paid(paid)
              .invoiceCount(count)
              .build());
    }
    return result;
  }

  private List<MonthlyPatients> buildPatientsByMonth(UUID hospitalId, OffsetDateTime since) {
    List<MonthlyPatients> result = new ArrayList<>();
    List<Object[]> rows = patientRepository.countByMonthRaw(hospitalId, since);
    for (Object[] row : rows) {
      String ym = (String) row[0];
      result.add(
          MonthlyPatients.builder()
              .yearMonth(ym)
              .month(monthLabel(ym))
              .newPatients(toLong(row[1]))
              .totalEpisodes(toLong(row[2]))
              .build());
    }
    return result;
  }

  private List<MonthlyEpisodes> buildEpisodesByMonth(UUID hospitalId, OffsetDateTime since) {
    List<MonthlyEpisodes> result = new ArrayList<>();
    List<Object[]> rows = episodeRepository.countByMonthRaw(hospitalId, since);
    for (Object[] row : rows) {
      String ym = (String) row[0];
      result.add(
          MonthlyEpisodes.builder()
              .yearMonth(ym)
              .month(monthLabel(ym))
              .scheduled(toLong(row[1]))
              .completed(toLong(row[2]))
              .cancelled(toLong(row[3]))
              .build());
    }
    return result;
  }

  // ------------------------------------------------
  // Helpers
  // ------------------------------------------------

  private BigDecimal safeSum(BigDecimal val) {
    return val != null ? val : BigDecimal.ZERO;
  }

  private BigDecimal toBD(Object val) {
    if (val == null) return BigDecimal.ZERO;
    if (val instanceof BigDecimal bd) return bd;
    return new BigDecimal(val.toString());
  }

  private long toLong(Object val) {
    if (val == null) return 0L;
    if (val instanceof Number n) return n.longValue();
    return Long.parseLong(val.toString());
  }

  private String monthLabel(String yearMonth) {
    // "2026-01" → "Jan"
    try {
      String[] parts = yearMonth.split("-");
      int month = Integer.parseInt(parts[1]);
      String[] months = {
        "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
        "Jul", "Ago", "Set", "Out", "Nov", "Dez"
      };
      return months[month - 1];
    } catch (Exception e) {
      return yearMonth;
    }
  }
}
