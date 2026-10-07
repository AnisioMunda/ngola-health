package ao.hospitalao.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

class LiquibaseChangelogTest {

  private static final Set<String> CHANGESETS_WITHOUT_ROLLBACK =
      Set.of("users-004-dev-pgcrypto", "users-005-dev-bootstrap-password");

  @Test
  void masterIncludesSqlChangesetsAndRollsBackReversibleChanges() throws Exception {
    var changelog = parseMasterChangelog();

    assertThat(changelog.getChangeSets()).isNotEmpty();
    assertThat(changelog.getChangeSets())
        // The extension can predate its changeset; the password changeset only validates input.
        .filteredOn(changeSet -> !CHANGESETS_WITHOUT_ROLLBACK.contains(changeSet.getId()))
        .allSatisfy(
            changeSet -> {
              assertThat(changeSet.getFilePath()).endsWith(".sql");
              assertThat(changeSet.getRollback()).isNotNull();
              assertThat(changeSet.getRollback().getChanges()).isNotEmpty();
            });
  }

  @Test
  void patientIdentifierMigrationIsIncludedAndReversible() throws Exception {
    var changelog = parseMasterChangelog();

    assertThat(changelog.getChangeSets())
        .filteredOn(
            changeSet -> changeSet.getId().equals("022-01-patient-identificadores-por-hospital"))
        .singleElement()
        .satisfies(
            changeSet -> {
              assertThat(changeSet.getFilePath())
                  .endsWith("changes/patients/003-hospital-scoped-identifiers.sql");
              assertThat(changeSet.getRollback()).isNotNull();
              assertThat(changeSet.getRollback().getChanges()).isNotEmpty();
            });
  }

  @Test
  void episodeTableMigrationIsIncludedAndReversible() throws Exception {
    var changelog = parseMasterChangelog();

    assertThat(changelog.getChangeSets())
        .filteredOn(changeSet -> changeSet.getId().equals("006-02-criar-tabela-episodes"))
        .singleElement()
        .satisfies(
            changeSet -> {
              assertThat(changeSet.getFilePath())
                  .endsWith("changes/episodes/001-create-episodes.sql");
              assertThat(changeSet.getRollback()).isNotNull();
              assertThat(changeSet.getRollback().getChanges()).isNotEmpty();
            });
  }

  @Test
  void triageTableMigrationIsIncludedAndReversible() throws Exception {
    var changelog = parseMasterChangelog();

    assertThat(changelog.getChangeSets())
        .filteredOn(changeSet -> changeSet.getId().equals("018-01-triage"))
        .singleElement()
        .satisfies(
            changeSet -> {
              assertThat(changeSet.getFilePath()).endsWith("changes/triage/001-triage.sql");
              assertThat(changeSet.getRollback()).isNotNull();
              assertThat(changeSet.getRollback().getChanges()).isNotEmpty();
            });
  }

  @Test
  void pharmacyTableMigrationsAreIncludedAndReversible() throws Exception {
    var changelog = parseMasterChangelog();

    assertChangesetIsIncludedAndReversible(
        changelog, "009-01-criar-tabela-medications", "changes/pharmacy/001-pharmacy.sql");
    assertChangesetIsIncludedAndReversible(
        changelog, "009-02-criar-tabela-stock-batches", "changes/pharmacy/001-pharmacy.sql");
    assertChangesetIsIncludedAndReversible(
        changelog, "009-03-criar-tabela-stock-movements", "changes/pharmacy/001-pharmacy.sql");
  }

  @Test
  void prescriptionTableMigrationsAreIncludedAndReversible() throws Exception {
    var changelog = parseMasterChangelog();

    assertChangesetIsIncludedAndReversible(
        changelog, "017-01-prescriptions", "changes/prescriptions/001-prescriptions.sql");
    assertChangesetIsIncludedAndReversible(
        changelog, "017-02-prescription-items", "changes/prescriptions/001-prescriptions.sql");
    assertChangesetIsIncludedAndReversible(
        changelog, "017-03-dispensations", "changes/prescriptions/001-prescriptions.sql");
    assertChangesetIsIncludedAndReversible(
        changelog,
        "hospitals-005-prescription-item-tenant",
        "changes/hospitals/002-tenant-scoped-records.sql");
    assertChangesetIsIncludedAndReversible(
        changelog,
        "hospitals-006-dispensation-tenant",
        "changes/hospitals/002-tenant-scoped-records.sql");
  }

  private void assertChangesetIsIncludedAndReversible(
      DatabaseChangeLog changelog, String id, String filePath) {
    assertThat(changelog.getChangeSets())
        .filteredOn(changeSet -> changeSet.getId().equals(id))
        .singleElement()
        .satisfies(
            changeSet -> {
              assertThat(changeSet.getFilePath()).endsWith(filePath);
              assertThat(changeSet.getRollback()).isNotNull();
              assertThat(changeSet.getRollback().getChanges()).isNotEmpty();
            });
  }

  private DatabaseChangeLog parseMasterChangelog() throws Exception {
    try (var resources = new ClassLoaderResourceAccessor()) {
      var path = "db/changelog/db.changelog-master.xml";
      return ChangeLogParserFactory.getInstance()
          .getParser(path, resources)
          .parse(path, new ChangeLogParameters(), resources);
    }
  }
}
