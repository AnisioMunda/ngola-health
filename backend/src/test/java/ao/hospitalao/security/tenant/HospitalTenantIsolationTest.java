package ao.hospitalao.security.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import ao.hospitalao.modules.patients.entity.Patient;
import ao.hospitalao.modules.patients.entity.Patient.Gender;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class HospitalTenantIsolationTest {

  private static final UUID DEFAULT_HOSPITAL =
      UUID.fromString("00000000-0000-0000-0000-000000000100");
  private static final UUID OTHER_HOSPITAL =
      UUID.fromString("00000000-0000-0000-0000-000000000200");

  @Container
  private static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:18-alpine")
          .withDatabaseName("hospitalao_tenant_test")
          .withUsername("hospitalao")
          .withPassword("test_password");

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private PatientRepository patientRepository;

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
  void createSecondHospital() {
    jdbcTemplate.update(
        """
            INSERT INTO hospitals (id, name, code, type, province, active)
            VALUES (?, 'Hospital de Teste', 'HT-002', 'HOSPITAL', 'Luanda', TRUE)
            ON CONFLICT (id) DO NOTHING
            """,
        OTHER_HOSPITAL);
  }

  @AfterEach
  void clearTenantContext() {
    TenantContext.clear();
  }

  @Test
  void readsAndWritesAreScopedAutomaticallyByHospital() {
    TenantContext.setCurrentHospital(DEFAULT_HOSPITAL);
    Patient firstPatient = patientRepository.save(newPatient("Paciente Um"));

    TenantContext.setCurrentHospital(OTHER_HOSPITAL);
    Patient secondPatient = patientRepository.save(newPatient("Paciente Dois"));

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT hospital_id FROM patients WHERE id = ?", UUID.class, firstPatient.getId()))
        .isEqualTo(DEFAULT_HOSPITAL);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT hospital_id FROM patients WHERE id = ?", UUID.class, secondPatient.getId()))
        .isEqualTo(OTHER_HOSPITAL);

    TenantContext.setCurrentHospital(DEFAULT_HOSPITAL);
    assertThat(patientRepository.findById(firstPatient.getId())).isPresent();
    assertThat(patientRepository.findById(secondPatient.getId())).isEmpty();
    assertThat(patientRepository.findAll())
        .extracting(Patient::getId)
        .contains(firstPatient.getId())
        .doesNotContain(secondPatient.getId());

    TenantContext.setCurrentHospital(OTHER_HOSPITAL);
    assertThat(patientRepository.findById(firstPatient.getId())).isEmpty();
    assertThat(patientRepository.findById(secondPatient.getId())).isPresent();
    patientRepository.deleteById(firstPatient.getId());

    TenantContext.setCurrentHospital(DEFAULT_HOSPITAL);
    assertThat(patientRepository.findById(firstPatient.getId())).isPresent();
  }

  @Test
  void missingHospitalContextDoesNotExposeTenantRows() {
    TenantContext.setCurrentHospital(DEFAULT_HOSPITAL);
    Patient patient = patientRepository.save(newPatient("Paciente Isolado"));
    TenantContext.clear();

    assertThat(patientRepository.findById(patient.getId())).isEmpty();
    assertThat(patientRepository.findAll()).doesNotContain(patient);
  }

  private Patient newPatient(String fullName) {
    return Patient.builder()
        .fullName(fullName)
        .birthDate(LocalDate.of(1990, 1, 1))
        .gender(Gender.FEMALE)
        .build();
  }
}
