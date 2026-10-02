package com.whoami.common;

import java.util.Arrays;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TiDB records failed MySQL DDL migrations in Flyway history. Clear only the
 * known failed V4 introduction migration so the corrected, split DDL can retry.
 */
@Configuration(proxyBeanMethods = false)
public class FlywayFailedV4RecoveryConfiguration {

    @Bean
    FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            MigrationInfo[] migrations = flyway.info().all();
            long failedCount = Arrays.stream(migrations)
                    .filter(migration -> migration.getState() == MigrationState.FAILED)
                    .count();
            boolean isOnlyFailedV4 = failedCount == 1 && Arrays.stream(migrations)
                    .anyMatch(FlywayFailedV4RecoveryConfiguration::isFailedExperienceMigration);
            if (isOnlyFailedV4) {
                flyway.repair();
            }
            flyway.migrate();
        };
    }

    private static boolean isFailedExperienceMigration(MigrationInfo migration) {
        return migration.getState() == MigrationState.FAILED
                && migration.getVersion() != null
                && "4".equals(migration.getVersion().getVersion())
                && "experience introductions".equals(migration.getDescription());
    }
}
