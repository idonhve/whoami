package com.whoami;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class TechCatalogMigrationTest {

    private static final Pattern TECH_STACK_ALTER = Pattern.compile(
            "(?is)ALTER\\s+TABLE\\s+tech_stack\\s+(.*?);"
    );

    @Test
    void createsCatalogIdempotentlyAndAddsItsIndexAfterTheColumns() throws IOException {
        String migration;
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("db/migration/V5__tech_catalog.sql")) {
            assertThat(input).as("V5 migration resource").isNotNull();
            migration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(migration).contains("CREATE TABLE IF NOT EXISTS tech_catalog");

        var matcher = TECH_STACK_ALTER.matcher(migration);
        assertThat(matcher.find()).isTrue();
        String addColumns = matcher.group(1);
        assertThat(matcher.find()).isTrue();
        String addIndex = matcher.group(1);
        assertThat(matcher.find()).isFalse();

        assertThat(addColumns)
                .contains("ADD COLUMN catalog_id", "ADD COLUMN icon_path")
                .doesNotContain("ADD KEY");
        assertThat(addIndex)
                .contains("ADD KEY idx_tech_stack_catalog_id (catalog_id)")
                .doesNotContain("ADD COLUMN");
    }
}
