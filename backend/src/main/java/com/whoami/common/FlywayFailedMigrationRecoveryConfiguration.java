package com.whoami.common;

import java.util.Arrays;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TiDB records failed MySQL DDL migrations in Flyway history. Remove only the
 * known failed experience/catalog migration entry before retrying corrected DDL.
 */
@Configuration(proxyBeanMethods = false)
public class FlywayFailedMigrationRecoveryConfiguration {

    @Bean
    FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            MigrationInfo[] migrations = flyway.info().all();
            long failedCount = Arrays.stream(migrations)
                    .filter(migration -> migration.getState() == MigrationState.FAILED)
                    .count();
            boolean isOnlyKnownFailedMigration = failedCount == 1 && Arrays.stream(migrations)
                    .anyMatch(FlywayFailedMigrationRecoveryConfiguration::isKnownFailedMigration);
            if (isOnlyKnownFailedMigration) {
                flyway.repair();
            }
            flyway.migrate();
        };
    }

    private static boolean isKnownFailedMigration(MigrationInfo migration) {
        if (migration.getState() != MigrationState.FAILED || migration.getVersion() == null) {
            return false;
        }
        String version = migration.getVersion().getVersion();
        String description = migration.getDescription();
        return ("4".equals(version) && "experience introductions".equals(description))
                || ("5".equals(version) && "tech catalog".equals(description));
    }
}
