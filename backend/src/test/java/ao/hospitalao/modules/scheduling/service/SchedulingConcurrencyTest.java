package ao.hospitalao.modules.scheduling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ao.hospitalao.modules.patients.entity.Patient;
import ao.hospitalao.modules.patients.entity.Patient.Gender;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.modules.scheduling.dto.SchedulingDtos.CreateAppointmentRequest;
import ao.hospitalao.security.tenant.TenantContext;
import java.time.LocalDate;
import java.time.LocalTime;
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

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class SchedulingConcurrencyTest {

  private static final LocalTime SLOT_START = LocalTime.of(10, 0);

  @Container
  private static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:18-alpine")
          .withDatabaseName("hospitalao_scheduling_test")
          .withUsername("hospitalao")
          .withPassword("test_password");

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private PatientRepository patientRepository;
  @Autowired private SchedulingService schedulingService;

  private UUID hospitalId;
  private UUID doctorId;
  private UUID firstPatientId;
  private UUID secondPatientId;
  private String doctorUsername;
  private LocalDate appointmentDate;

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
  void createSchedulingRecords() {
    hospitalId =
        jdbcTemplate.queryForObject("SELECT id FROM hospitals WHERE code = 'HCL-001'", UUID.class);
    doctorId = UUID.randomUUID();
    doctorUsername = "scheduling-test-" + UUID.randomUUID();
    appointmentDate = LocalDate.now().plusDays(3);
    TenantContext.setCurrentHospital(hospitalId);

    jdbcTemplate.update(
        """
            INSERT INTO users (
                id, hospital_id, full_name, username, password_hash, register_status,
                must_change_password, failed_login_attempts, created_at, updated_at
            ) VALUES (?, ?, 'Scheduling Doctor', ?, 'test-hash', 'ACTIVE', FALSE, 0, NOW(), NOW())
            """,
        doctorId,
        hospitalId,
        doctorUsername);

    firstPatientId = createPatient("First Patient");
    secondPatientId = createPatient("Second Patient");
  }

  @AfterEach
  void removeSchedulingRecords() {
    jdbcTemplate.update("DELETE FROM appointments WHERE doctor_id = ?", doctorId);
    jdbcTemplate.update("DELETE FROM doctor_schedules WHERE doctor_id = ?", doctorId);
    jdbcTemplate.update("DELETE FROM patients WHERE id IN (?, ?)", firstPatientId, secondPatientId);
    jdbcTemplate.update("DELETE FROM users WHERE id = ?", doctorId);
    TenantContext.clear();
    SecurityContextHolder.clearContext();
  }

  @Test
  void concurrentBookingsCannotExceedSinglePatientSlotCapacity() throws Exception {
    createSchedule(1);

    List<Boolean> outcomes =
        attemptTwoBookings(requestFor(firstPatientId), requestFor(secondPatientId));

    assertThat(outcomes).containsExactlyInAnyOrder(true, false);
    assertThat(activeAppointments()).isEqualTo(1);
    assertThat(slotPositions()).containsExactly(1);
  }

  @Test
  void concurrentBookingsUseDistinctPositionsWithinConfiguredCapacity() throws Exception {
    createSchedule(2);

    List<Boolean> outcomes =
        attemptTwoBookings(requestFor(firstPatientId), requestFor(secondPatientId));

    assertThat(outcomes).containsOnly(true);
    assertThat(activeAppointments()).isEqualTo(2);
    assertThat(slotPositions()).containsExactly(1, 2);
  }

  @Test
  void samePatientCannotBookTheSameSlotTwice() throws Exception {
    createSchedule(2);

    assertThat(attemptBooking(requestFor(firstPatientId))).isTrue();
    assertThat(attemptBooking(requestFor(firstPatientId))).isFalse();
    assertThat(activeAppointments()).isEqualTo(1);
  }

  @Test
  void cancelledAppointmentReleasesItsSlotPosition() {
    createSchedule(1);
    assertThat(attemptBooking(requestFor(firstPatientId))).isTrue();

    jdbcTemplate.update(
        """
            UPDATE appointments
            SET status = 'CANCELLED'
            WHERE doctor_id = ? AND appointment_date = ? AND start_time = ?
            """,
        doctorId,
        appointmentDate,
        SLOT_START);

    assertThat(attemptBooking(requestFor(secondPatientId))).isTrue();
    assertThat(activeAppointments()).isEqualTo(1);
    assertThat(slotPositions()).containsExactly(1);
  }

  @Test
  void databaseConstraintRejectsDuplicateActiveSlotPosition() {
    createSchedule(2);
    assertThat(attemptBooking(requestFor(firstPatientId))).isTrue();

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                        INSERT INTO appointments (
                            id, hospital_id, patient_id, doctor_id, appointment_date,
                            start_time, end_time, slot_position, status, appointment_type,
                            reason, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, 1, 'SCHEDULED', 'OUTPATIENT',
                                  'Duplicate slot position', NOW(), NOW())
                        """,
                    UUID.randomUUID(),
                    hospitalId,
                    secondPatientId,
                    doctorId,
                    appointmentDate,
                    SLOT_START,
                    SLOT_START.plusMinutes(30)))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThat(activeAppointments()).isEqualTo(1);
  }

  private List<Boolean> attemptTwoBookings(
      CreateAppointmentRequest first, CreateAppointmentRequest second) throws Exception {
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    var executor = Executors.newFixedThreadPool(2);
    try {
      var attempts =
          List.of(
              executor.submit(() -> attemptBooking(first, ready, start)),
              executor.submit(() -> attemptBooking(second, ready, start)));
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      return attempts.stream()
          .map(
              future -> {
                try {
                  return future.get(30, TimeUnit.SECONDS);
                } catch (Exception exception) {
                  throw new AssertionError("Concurrent booking failed unexpectedly", exception);
                }
              })
          .toList();
    } finally {
      start.countDown();
      executor.shutdownNow();
    }
  }

  private boolean attemptBooking(CreateAppointmentRequest request) {
    return attemptBooking(request, null, null);
  }

  private boolean attemptBooking(
      CreateAppointmentRequest request, CountDownLatch ready, CountDownLatch start) {
    TenantContext.setCurrentHospital(hospitalId);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(doctorUsername, "test", List.of()));
    try {
      if (ready != null) {
        ready.countDown();
        assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
      }
      schedulingService.create(request);
      return true;
    } catch (ResponseStatusException exception) {
      if (exception.getStatusCode().value() == 409) {
        return false;
      }
      throw exception;
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Concurrent booking was interrupted", exception);
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

  private void createSchedule(int capacity) {
    jdbcTemplate.update(
        """
            INSERT INTO doctor_schedules (
                id, hospital_id, doctor_id, day_of_week, start_time, end_time,
                slot_duration_minutes, max_patients_per_slot, active, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, 30, ?, TRUE, NOW())
            """,
        UUID.randomUUID(),
        hospitalId,
        doctorId,
        appointmentDate.getDayOfWeek().getValue() - 1,
        SLOT_START,
        SLOT_START.plusMinutes(60),
        capacity);
  }

  private CreateAppointmentRequest requestFor(UUID patientId) {
    var request = new CreateAppointmentRequest();
    request.setPatientId(patientId);
    request.setDoctorId(doctorId);
    request.setAppointmentDate(appointmentDate);
    request.setStartTime(SLOT_START);
    request.setReason("Consulta de teste");
    return request;
  }

  private int activeAppointments() {
    return jdbcTemplate.queryForObject(
        """
            SELECT COUNT(*)
            FROM appointments
            WHERE doctor_id = ?
              AND appointment_date = ?
              AND start_time = ?
              AND status NOT IN ('CANCELLED', 'NO_SHOW')
            """,
        Integer.class,
        doctorId,
        appointmentDate,
        SLOT_START);
  }

  private List<Integer> slotPositions() {
    return jdbcTemplate.queryForList(
        """
            SELECT slot_position
            FROM appointments
            WHERE doctor_id = ?
              AND appointment_date = ?
              AND start_time = ?
              AND status NOT IN ('CANCELLED', 'NO_SHOW')
            ORDER BY slot_position
            """,
        Integer.class,
        doctorId,
        appointmentDate,
        SLOT_START);
  }
}
