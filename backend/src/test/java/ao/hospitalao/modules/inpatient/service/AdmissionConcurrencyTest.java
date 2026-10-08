package ao.hospitalao.modules.inpatient.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ao.hospitalao.application.inpatient.AdmissionService;
import ao.hospitalao.modules.inpatient.dto.InpatientDtos.CreateAdmissionRequest;
import ao.hospitalao.modules.patients.entity.Patient;
import ao.hospitalao.modules.patients.entity.Patient.Gender;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.security.tenant.TenantContext;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
import org.springframework.dao.DataIntegrityViolationException;
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

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class AdmissionConcurrencyTest {

  @Container
  private static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:18-alpine")
          .withDatabaseName("hospitalao_inpatient_test")
          .withUsername("hospitalao")
          .withPassword("test_password");

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private PatientRepository patientRepository;
  @Autowired private AdmissionService admissionService;

  private UUID hospitalId;
  private UUID doctorId;
  private UUID firstPatientId;
  private UUID secondPatientId;
  private UUID wardId;
  private UUID bedId;
  private UUID secondBedId;
  private String doctorUsername;

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
  void createInpatientRecords() {
    hospitalId =
        jdbcTemplate.queryForObject("SELECT id FROM hospitals WHERE code = 'HCL-001'", UUID.class);
    doctorId = UUID.randomUUID();
    doctorUsername = "inpatient-test-" + UUID.randomUUID();
    wardId = UUID.randomUUID();
    bedId = UUID.randomUUID();
    secondBedId = UUID.randomUUID();
    TenantContext.setCurrentHospital(hospitalId);

    jdbcTemplate.update(
        """
            INSERT INTO users (
                id, hospital_id, full_name, username, password_hash, register_status,
                must_change_password, failed_login_attempts, created_at, updated_at
            ) VALUES (?, ?, 'Inpatient Doctor', ?, 'test-hash', 'ACTIVE', FALSE, 0, NOW(), NOW())
            """,
        doctorId,
        hospitalId,
        doctorUsername);

    firstPatientId = createPatient("First Inpatient");
    secondPatientId = createPatient("Second Inpatient");
    jdbcTemplate.update(
        """
            INSERT INTO wards (
                id, hospital_id, name, code, type, total_beds, active, created_at
            ) VALUES (?, ?, 'Inpatient Test Ward', ?, 'GENERAL', 2, TRUE, NOW())
            """,
        wardId,
        hospitalId,
        "W-" + wardId.toString().substring(0, 8));
    jdbcTemplate.update(
        """
            INSERT INTO beds (id, ward_id, hospital_id, bed_number, status, type, active)
            VALUES (?, ?, ?, '01', 'AVAILABLE', 'STANDARD', TRUE)
            """,
        bedId,
        wardId,
        hospitalId);
    jdbcTemplate.update(
        """
            INSERT INTO beds (id, ward_id, hospital_id, bed_number, status, type, active)
            VALUES (?, ?, ?, '02', 'AVAILABLE', 'STANDARD', TRUE)
            """,
        secondBedId,
        wardId,
        hospitalId);
  }

  @AfterEach
  void removeInpatientRecords() {
    jdbcTemplate.update(
        "DELETE FROM admissions WHERE bed_id = ? OR patient_id IN (?, ?)",
        bedId,
        firstPatientId,
        secondPatientId);
    jdbcTemplate.update("DELETE FROM beds WHERE id IN (?, ?)", bedId, secondBedId);
    jdbcTemplate.update("DELETE FROM wards WHERE id = ?", wardId);
    jdbcTemplate.update("DELETE FROM patients WHERE id IN (?, ?)", firstPatientId, secondPatientId);
    jdbcTemplate.update("DELETE FROM users WHERE id = ?", doctorId);
    TenantContext.clear();
    SecurityContextHolder.clearContext();
  }

  @Test
  void concurrentAdmissionsCannotOccupyTheSameBed() throws Exception {
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    var executor = Executors.newFixedThreadPool(2);
    try {
      var attempts =
          List.of(
              executor.submit(() -> attemptAdmission(firstPatientId, ready, start)),
              executor.submit(() -> attemptAdmission(secondPatientId, ready, start)));
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      List<Boolean> outcomes =
          attempts.stream()
              .map(
                  future -> {
                    try {
                      return future.get(30, TimeUnit.SECONDS);
                    } catch (Exception exception) {
                      throw new AssertionError(
                          "Concurrent admission failed unexpectedly", exception);
                    }
                  })
              .toList();

      assertThat(outcomes).containsExactlyInAnyOrder(true, false);
      assertThat(activeAdmissionsForBed()).isEqualTo(1);
      assertThat(bedStatus()).isEqualTo("OCCUPIED");
    } finally {
      start.countDown();
      executor.shutdownNow();
    }
  }

  @Test
  void databaseRejectsTwoActiveAdmissionsForTheSameBed() {
    insertAdmission(firstPatientId, bedId);

    assertThatThrownBy(() -> insertAdmission(secondPatientId, bedId))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(activeAdmissionsForBed()).isEqualTo(1);
  }

  @Test
  void databaseRejectsTwoActiveAdmissionsForTheSamePatient() {
    insertAdmission(firstPatientId, bedId);

    assertThatThrownBy(() -> insertAdmission(firstPatientId, secondBedId))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private boolean attemptAdmission(UUID patientId, CountDownLatch ready, CountDownLatch start) {
    TenantContext.setCurrentHospital(hospitalId);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(doctorUsername, "test", List.of()));
    try {
      ready.countDown();
      assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
      admissionService.admit(requestFor(patientId));
      return true;
    } catch (ResponseStatusException exception) {
      if (exception.getStatusCode().value() == 409) return false;
      throw exception;
    } catch (DataIntegrityViolationException exception) {
      return false;
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Concurrent admission was interrupted", exception);
    } finally {
      TenantContext.clear();
      SecurityContextHolder.clearContext();
    }
  }

  private UUID createPatient(String name) {
    return patientRepository
        .save(
            Patient.builder()
                .fullName(name)
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender(Gender.FEMALE)
                .build())
        .getId();
  }

  private CreateAdmissionRequest requestFor(UUID patientId) {
    var request = new CreateAdmissionRequest();
    request.setPatientId(patientId);
    request.setBedId(bedId);
    request.setResponsibleDoctorId(doctorId);
    request.setAdmissionReason("Internamento de teste");
    return request;
  }

  private void insertAdmission(UUID patientId, UUID admissionBedId) {
    OffsetDateTime now = OffsetDateTime.now();
    jdbcTemplate.update(
        """
            INSERT INTO admissions (
                id, hospital_id, patient_id, bed_id, ward_id, responsible_doctor_id,
                status, admission_date, admission_reason, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', ?, 'Internamento de teste', ?, ?)
            """,
        UUID.randomUUID(),
        hospitalId,
        patientId,
        admissionBedId,
        wardId,
        doctorId,
        now,
        now,
        now);
  }

  private int activeAdmissionsForBed() {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM admissions WHERE bed_id = ? AND status = 'ACTIVE'",
        Integer.class,
        bedId);
  }

  private String bedStatus() {
    return jdbcTemplate.queryForObject("SELECT status FROM beds WHERE id = ?", String.class, bedId);
  }
}
