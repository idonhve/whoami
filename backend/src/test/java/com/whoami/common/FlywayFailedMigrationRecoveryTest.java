package com.whoami.common;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;

class FlywayFailedMigrationRecoveryTest {

    @Test
    void repairsTheFailedExperienceMigrationBeforeRetryingMigrations() {
        assertRepairBeforeMigrate("4", "experience introductions");
    }

    @Test
    void repairsTheFailedCatalogMigrationBeforeRetryingMigrations() {
        assertRepairBeforeMigrate("5", "tech catalog");
    }

    @Test
    void doesNotRepairOtherFailedMigrations() {
        Flyway flyway = mock(Flyway.class);
        MigrationInfo failedMigration = migration("6", "unrelated migration", MigrationState.FAILED);
        MigrationInfoService migrationInfo = info(failedMigration);
        when(flyway.info()).thenReturn(migrationInfo);
        FlywayMigrationStrategy strategy = new FlywayFailedMigrationRecoveryConfiguration()
                .flywayMigrationStrategy();

        strategy.migrate(flyway);

        verify(flyway, never()).repair();
        verify(flyway).migrate();
    }

    private void assertRepairBeforeMigrate(String version, String description) {
        Flyway flyway = mock(Flyway.class);
        MigrationInfo failedMigration = migration(version, description, MigrationState.FAILED);
        MigrationInfoService migrationInfo = info(failedMigration);
        when(flyway.info()).thenReturn(migrationInfo);
        FlywayMigrationStrategy strategy = new FlywayFailedMigrationRecoveryConfiguration()
                .flywayMigrationStrategy();

        strategy.migrate(flyway);

        InOrder order = inOrder(flyway);
        order.verify(flyway).repair();
        order.verify(flyway).migrate();
    }

    private MigrationInfoService info(MigrationInfo... migrations) {
        MigrationInfoService info = mock(MigrationInfoService.class);
        when(info.all()).thenReturn(migrations);
        return info;
    }

    private MigrationInfo migration(String version, String description, MigrationState state) {
        MigrationInfo migration = mock(MigrationInfo.class);
        when(migration.getVersion()).thenReturn(MigrationVersion.fromVersion(version));
        when(migration.getDescription()).thenReturn(description);
        when(migration.getState()).thenReturn(state);
        return migration;
    }
}
