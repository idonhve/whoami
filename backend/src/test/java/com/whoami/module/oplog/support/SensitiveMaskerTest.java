package com.whoami.module.oplog.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class SensitiveMaskerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void masksPasswordAndTokenFieldsRecursively() throws Exception {
        JsonNode node = objectMapper.readTree(
                "{\"username\":\"admin\",\"password\":\"plain-text\",\"nested\":{\"accessToken\":\"abc\",\"note\":\"ok\"}}");

        JsonNode masked = SensitiveMasker.mask(node);

        assertThat(masked.path("username").asText()).isEqualTo("admin");
        assertThat(masked.path("password").asText()).isEqualTo("***");
        assertThat(masked.path("nested").path("accessToken").asText()).isEqualTo("***");
        assertThat(masked.path("nested").path("note").asText()).isEqualTo("ok");
    }

    @Test
    void masksFieldsInsideArrays() throws Exception {
        JsonNode node = objectMapper.readTree("[{\"secret\":\"s1\"},{\"value\":\"v1\"}]");

        JsonNode masked = SensitiveMasker.mask(node);

        assertThat(masked.get(0).path("secret").asText()).isEqualTo("***");
        assertThat(masked.get(1).path("value").asText()).isEqualTo("v1");
    }

    @Test
    void nullInputReturnsNull() {
        assertThat(SensitiveMasker.mask(null)).isNull();
    }

    @Test
    void nonSensitiveJsonPassesThrough() throws Exception {
        JsonNode node = objectMapper.readTree("{\"key\":\"domain\",\"value\":\"whoami.dev\"}");

        JsonNode masked = SensitiveMasker.mask(node);

        assertThat(masked.path("key").asText()).isEqualTo("domain");
        assertThat(masked.path("value").asText()).isEqualTo("whoami.dev");
    }
}
