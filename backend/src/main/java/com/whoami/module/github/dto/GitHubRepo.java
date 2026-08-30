package com.whoami.module.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/** GitHub REST API /users/{owner}/repos 响应项（只取本站需要的字段；snake_case 显式映射） */
public record GitHubRepo(
        Long id,
        String name,
        @JsonProperty("full_name") String fullName,
        String description,
        String language,
        @JsonProperty("stargazers_count") Integer stargazersCount,
        @JsonProperty("forks_count") Integer forksCount,
        @JsonProperty("html_url") String htmlUrl,
        @JsonProperty("pushed_at") LocalDateTime pushedAt) {
}
