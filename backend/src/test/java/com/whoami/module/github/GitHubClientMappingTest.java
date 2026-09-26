package com.whoami.module.github;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.whoami.module.github.dto.GitHubRepo;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * GitHub JSON 是 snake_case，record 字段是 camelCase；
 * 若 @JsonProperty 映射缺失会静默变成 null（线上表现为 full_name 插库报错），
 * 用真实 GitHub 响应片段钉住这条契约。
 */
class GitHubClientMappingTest {

    /** 与 Spring Boot 自动配置一致：jsr310 模块已注册 */
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final String REPO_JSON = """
            [{
              "id": 1001,
              "name": "whoami",
              "full_name": "idonhve/whoami",
              "description": "terminal portfolio",
              "language": "Java",
              "stargazers_count": 12,
              "forks_count": 3,
              "html_url": "https://github.com/idonhve/whoami",
              "pushed_at": "2026-08-30T04:14:26Z"
            }]
            """;

    @Test
    void snakeCase字段全部映射到record() throws Exception {
        GitHubRepo[] repos = mapper.readValue(REPO_JSON, GitHubRepo[].class);
        GitHubRepo repo = repos[0];

        assertThat(repo.id()).isEqualTo(1001L);
        assertThat(repo.name()).isEqualTo("whoami");
        assertThat(repo.fullName()).as("full_name 需 @JsonProperty 显式映射").isEqualTo("idonhve/whoami");
        assertThat(repo.description()).isEqualTo("terminal portfolio");
        assertThat(repo.language()).isEqualTo("Java");
        assertThat(repo.stargazersCount()).as("stargazers_count 需 @JsonProperty 显式映射").isEqualTo(12);
        assertThat(repo.forksCount()).as("forks_count 需 @JsonProperty 显式映射").isEqualTo(3);
        assertThat(repo.htmlUrl()).as("html_url 需 @JsonProperty 显式映射").isEqualTo("https://github.com/idonhve/whoami");
        assertThat(repo.pushedAt()).as("pushed_at 需 @JsonProperty 显式映射").isEqualTo(LocalDateTime.of(2026, 8, 30, 4, 14, 26));
    }
}
