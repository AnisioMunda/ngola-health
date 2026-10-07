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

  private DatabaseChangeLog parseMasterChangelog() throws Exception {
    try (var resources = new ClassLoaderResourceAccessor()) {
      var path = "db/changelog/db.changelog-master.xml";
      return ChangeLogParserFactory.getInstance()
          .getParser(path, resources)
          .parse(path, new ChangeLogParameters(), resources);
    }
  }
}
