package com.whoami;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ExperienceIntroductionsMigrationTest {

    private static final Pattern EXPERIENCE_ALTER = Pattern.compile(
            "(?is)ALTER\\s+TABLE\\s+experience\\s+(.*?);"
    );

    @Test
    void addsEachIntroductionColumnInItsOwnAlterForTiDbCompatibility() throws IOException {
        String migration;
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("db/migration/V4__experience_introductions.sql")) {
            assertThat(input).as("V4 migration resource").isNotNull();
            migration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        var matcher = EXPERIENCE_ALTER.matcher(migration);
        assertThat(matcher.find()).isTrue();
        String firstAlter = matcher.group(1);
        assertThat(matcher.find()).isTrue();
        String secondAlter = matcher.group(1);

        assertThat(matcher.find()).isFalse();
        assertThat(firstAlter).contains("ADD COLUMN company_intro").doesNotContain("project_intro");
        assertThat(secondAlter)
                .contains("ADD COLUMN project_intro", "AFTER company_intro")
                .doesNotContain("ADD COLUMN company_intro");
    }
}
