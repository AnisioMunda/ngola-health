package ao.hospitalao;

import static org.assertj.core.api.Assertions.assertThat;

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
class HospitalAoStartupTest {

  @Container
  private static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:18-alpine")
          .withDatabaseName("hospitalao_test")
          .withUsername("hospitalao")
          .withPassword("test_password");

  @Autowired private JdbcTemplate jdbcTemplate;

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

  @Test
  void startsApplicationAndAppliesEveryMigrationToAnEmptyDatabase() {
    Long appliedChangesets =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM databasechangelog", Long.class);

    assertThat(appliedChangesets).isEqualTo(80L);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'hospitals'",
                Long.class))
        .isEqualTo(1L);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT name FROM hospitals WHERE code = 'HCL-001'", String.class))
        .isEqualTo("Hospital Central de Luanda");
    assertThat(jdbcTemplate.queryForList("SELECT name FROM roles", String.class))
        .containsExactlyInAnyOrder(
            "ADMIN",
            "DOCTOR",
            "NURSE",
            "RECEPTIONIST",
            "PHARMACIST",
            "FINANCIAL",
            "MANAGER",
            "LAB_TECHNICIAN",
            "SUPER_ADMIN");
    assertThat(
            jdbcTemplate.queryForObject(
                """
                    SELECT COUNT(*)
                    FROM information_schema.tables
                    WHERE table_schema = current_schema()
                      AND table_name IN ('users', 'roles', 'permissions', 'user_roles', 'role_permissions')
                    """,
                Long.class))
        .isEqualTo(5L);
    assertThat(
            jdbcTemplate.queryForObject(
                """
                    SELECT COUNT(*)
                    FROM information_schema.columns
                    WHERE table_schema = current_schema()
                      AND column_name = 'hospital_id'
                    """,
                Long.class))
        .isEqualTo(34L);
    assertThat(
            jdbcTemplate.queryForObject(
                """
                    SELECT COUNT(*)
                    FROM information_schema.columns
                    WHERE table_schema = current_schema()
                      AND table_name = 'invoices'
                      AND column_name = 'currency'
                      AND data_type = 'character varying'
                      AND character_maximum_length = 3
                      AND is_nullable = 'NO'
                      AND column_default LIKE '%AOA%'
                    """,
                Long.class))
        .isEqualTo(1L);
    assertThat(
            jdbcTemplate.queryForObject(
                """
                    SELECT COUNT(*)
                    FROM information_schema.columns
                    WHERE table_schema = current_schema()
                      AND table_name = 'users'
                      AND column_name = 'teams_user_id'
                    """,
                Long.class))
        .isEqualTo(1L);
    assertThat(
            jdbcTemplate.queryForObject(
                """
                    SELECT COUNT(*)
                    FROM information_schema.columns
                    WHERE table_schema = current_schema()
                      AND table_name = 'telemedicine_sessions'
                      AND column_name IN ('provider_meeting_id', 'provider_organizer_id', 'room_url')
                    """,
                Long.class))
        .isEqualTo(3L);
  }
}
