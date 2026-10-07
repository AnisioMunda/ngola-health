package ao.hospitalao.db;

import static org.assertj.core.api.Assertions.assertThat;

import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

class LiquibaseChangelogTest {

  @Test
  void masterIncludesOnlySqlChangesetsThatCanBeRolledBack() throws Exception {
    var changelog = parseMasterChangelog();

    assertThat(changelog.getChangeSets()).isNotEmpty();
    assertThat(changelog.getChangeSets())
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

  private DatabaseChangeLog parseMasterChangelog() throws Exception {
    try (var resources = new ClassLoaderResourceAccessor()) {
      var path = "db/changelog/db.changelog-master.xml";
      return ChangeLogParserFactory.getInstance()
          .getParser(path, resources)
          .parse(path, new ChangeLogParameters(), resources);
    }
  }
}
