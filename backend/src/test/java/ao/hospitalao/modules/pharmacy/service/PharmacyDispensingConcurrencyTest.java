package ao.hospitalao.modules.pharmacy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ao.hospitalao.modules.pharmacy.dto.PharmacyDtos.DispenseRequest;
import ao.hospitalao.modules.pharmacy.dto.PharmacyDtos.StockBatchResponse;
import ao.hospitalao.security.tenant.TenantContext;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class PharmacyDispensingConcurrencyTest {

  private static final ZoneId ANGOLA_ZONE = ZoneId.of("Africa/Luanda");

  @Container
  private static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:18-alpine")
          .withDatabaseName("hospitalao_pharmacy_test")
          .withUsername("hospitalao")
          .withPassword("test_password");

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private PharmacyService pharmacyService;

  private UUID hospitalId;
  private UUID medicationId;
  private UUID userId;
  private String username;

  @DynamicPropertySource
  static void configurePostgres(DynamicPropertyRegistry properties) {
    properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    properties.add("spring.datasource.username", POSTGRES::getUsername);
    properties.add("spring.datasource.password", POSTGRES::getPassword);
    properties.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    properties.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    properties.add(
        "spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    properties.add("spring.liquibase.enabled", () -> true);
  }

  @BeforeEach
  void createPharmacyRecords() {
    hospitalId =
        jdbcTemplate.queryForObject("SELECT id FROM hospitals WHERE code = 'HCL-001'", UUID.class);
    medicationId = UUID.randomUUID();
    userId = UUID.randomUUID();
    username = "pharmacy-test-" + UUID.randomUUID();

    jdbcTemplate.update(
        """
            INSERT INTO users (
                id, hospital_id, full_name, username, password_hash, register_status,
                must_change_password, failed_login_attempts, created_at, updated_at
            ) VALUES (?, ?, 'Pharmacist Test', ?, 'test-hash', 'ACTIVE', FALSE, 0, NOW(), NOW())
            """,
        userId,
        hospitalId,
        username);
    jdbcTemplate.update(
        """
            INSERT INTO medications (
                id, hospital_id, name, dosage_form, unit, requires_prescription,
                min_stock_level, active, created_at
            ) VALUES (?, ?, 'Medication Test', 'TABLET', 'comprimido', FALSE, 0, TRUE, NOW())
            """,
        medicationId,
        hospitalId);
  }

  @AfterEach
  void removePharmacyRecords() {
    jdbcTemplate.update(
        """
            DELETE FROM stock_movements
            WHERE batch_id IN (SELECT id FROM stock_batches WHERE medication_id = ?)
            """,
        medicationId);
    jdbcTemplate.update("DELETE FROM stock_batches WHERE medication_id = ?", medicationId);
    jdbcTemplate.update("DELETE FROM medications WHERE id = ?", medicationId);
    jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
    TenantContext.clear();
    SecurityContextHolder.clearContext();
  }

  @Test
  void dispensesFromTheEarliestExpiringBatchFirst() {
    UUID earliestBatchId = insertBatch(LocalDate.now(ANGOLA_ZONE).plusDays(5), 2);
    UUID laterBatchId = insertBatch(LocalDate.now(ANGOLA_ZONE).plusDays(20), 5);

    TenantContext.setCurrentHospital(hospitalId);
    authenticate();
    pharmacyService.dispense(dispenseRequest(3));

    assertThat(quantityAvailable(earliestBatchId)).isZero();
    assertThat(quantityAvailable(laterBatchId)).isEqualTo(4);
  }

  @Test
  void reportsStockExpiringWithinTheInclusiveThirtyDayWindow() {
    LocalDate today = LocalDate.now(ANGOLA_ZONE);
    UUID expiringBatchId = insertBatch(today.plusDays(30), 2);
    UUID laterBatchId = insertBatch(today.plusDays(31), 5);

    TenantContext.setCurrentHospital(hospitalId);
    List<StockBatchResponse> expiringBatches = pharmacyService.findExpiringSoon(30);

    assertThat(expiringBatches)
        .extracting(StockBatchResponse::getId)
        .containsExactly(expiringBatchId);
    assertThat(expiringBatches.getFirst().isExpiringSoon()).isTrue();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT quantity_available FROM stock_batches WHERE id = ?",
                Integer.class,
                laterBatchId))
        .isEqualTo(5);
  }

  @Test
  void rejectsInvalidExpiryAlertWindows() {
    assertThatThrownBy(() -> pharmacyService.findExpiringSoon(0))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("entre 1 e 365");
  }

  @Test
  void concurrentDispensingCannotOversellAvailableStock() throws Exception {
    UUID batchId = insertBatch(LocalDate.now(ANGOLA_ZONE).plusDays(20), 5);
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    var executor = Executors.newFixedThreadPool(2);

    try {
      var attempts =
          List.of(
              executor.submit(() -> attemptDispense(ready, start)),
              executor.submit(() -> attemptDispense(ready, start)));
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();

      long successfulDispensations =
          attempts.stream()
              .map(
                  future -> {
                    try {
                      return future.get(30, TimeUnit.SECONDS);
                    } catch (Exception exception) {
                      throw new AssertionError(
                          "Concurrent dispensing failed unexpectedly", exception);
                    }
                  })
              .filter(Boolean::booleanValue)
              .count();

      assertThat(successfulDispensations).isEqualTo(1);
      assertThat(quantityAvailable(batchId)).isEqualTo(1);
      assertThat(
              jdbcTemplate.queryForObject(
                  """
                      SELECT COALESCE(SUM(quantity), 0)
                      FROM stock_movements
                      WHERE batch_id = ? AND movement_type = 'DISPENSE'
                      """,
                  Integer.class,
                  batchId))
          .isEqualTo(-4);
    } finally {
      start.countDown();
      executor.shutdownNow();
    }
  }

  private boolean attemptDispense(CountDownLatch ready, CountDownLatch start)
      throws InterruptedException {
    TenantContext.setCurrentHospital(hospitalId);
    authenticate();
    try {
      ready.countDown();
      assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
      pharmacyService.dispense(dispenseRequest(4));
      return true;
    } catch (ResponseStatusException exception) {
      if (exception.getStatusCode().value() == 409) return false;
      throw exception;
    } finally {
      TenantContext.clear();
      SecurityContextHolder.clearContext();
    }
  }

  private void authenticate() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(username, "test", List.of()));
  }

  private UUID insertBatch(LocalDate expiryDate, int quantity) {
    UUID batchId = UUID.randomUUID();
    jdbcTemplate.update(
        """
            INSERT INTO stock_batches (
                id, hospital_id, medication_id, batch_number, expiry_date,
                quantity_received, quantity_available, received_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
            """,
        batchId,
        hospitalId,
        medicationId,
        "LOT-" + batchId,
        expiryDate,
        quantity,
        quantity);
    return batchId;
  }

  private int quantityAvailable(UUID batchId) {
    return jdbcTemplate.queryForObject(
        "SELECT quantity_available FROM stock_batches WHERE id = ?", Integer.class, batchId);
  }

  private DispenseRequest dispenseRequest(int quantity) {
    var request = new DispenseRequest();
    request.setMedicationId(medicationId);
    request.setQuantity(quantity);
    return request;
  }
}
