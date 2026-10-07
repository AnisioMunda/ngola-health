package ao.hospitalao.db;

import liquibase.changelog.ChangeLogParameters;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LiquibaseChangelogTest {

    @Test
    void masterIncludesOnlySqlChangesetsThatCanBeRolledBack() throws Exception {
        try (var resources = new ClassLoaderResourceAccessor()) {
            var path = "db/changelog/db.changelog-master.xml";
            var changelog = ChangeLogParserFactory.getInstance()
                .getParser(path, resources)
                .parse(path, new ChangeLogParameters(), resources);

            assertThat(changelog.getChangeSets()).isNotEmpty();
            assertThat(changelog.getChangeSets())
                .allSatisfy(changeSet -> {
                    assertThat(changeSet.getFilePath()).endsWith(".sql");
                    assertThat(changeSet.getRollback()).isNotNull();
                    assertThat(changeSet.getRollback().getChanges()).isNotEmpty();
                });
        }
    }
}
