package com.whoami.module.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whoami.module.github.config.GitHubProperties;
import com.whoami.module.github.dto.GitHubRepo;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * GitHub REST API 客户端（Spec 04）：GET /users/{owner}/repos?per_page=100&sort=pushed 逐页拉全。
 * 前台永不直连 GitHub（ADR-0001），只有本客户端与 SyncService 走外网。
 */
@Component
public class GitHubClient {

    private static final int PER_PAGE = 100;
    /** 单次同步最多 30 页（3000 仓库），防异常账号拖垮同步任务 */
    private static final int MAX_PAGES = 30;

    private final GitHubProperties properties;
    private final RestClient restClient;

    public GitHubClient(GitHubProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.restClient = RestClient.builder()
                .baseUrl(properties.apiBase())
                .requestFactory(factory)
                // 换用 Spring 上下文的 ObjectMapper（带 jsr310，可解析 pushed_at）
                .messageConverters(converters -> converters.stream()
                        .filter(MappingJackson2HttpMessageConverter.class::isInstance)
                        .map(c -> (MappingJackson2HttpMessageConverter) c)
                        .forEach(c -> c.setObjectMapper(objectMapper)))
                .build();
    }

    public List<GitHubRepo> fetchOwnerRepos() {
        if (properties.tokenMissing()) {
            throw new GitHubSyncException("未配置 GITHUB_TOKEN（只需只读公开仓库 PAT），无法同步");
        }
        if (properties.ownerMissing()) {
            throw new GitHubSyncException("未配置 GITHUB_OWNER（站主 GitHub 用户名），无法同步");
        }
        List<GitHubRepo> all = new ArrayList<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            List<GitHubRepo> batch = fetchPage(page);
            if (batch == null || batch.isEmpty()) {
                break;
            }
            all.addAll(batch);
            if (batch.size() < PER_PAGE) {
                break;
            }
        }
        return all;
    }

    private List<GitHubRepo> fetchPage(int page) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/users/{owner}/repos")
                            .queryParam("per_page", PER_PAGE)
                            .queryParam("sort", "pushed")
                            .queryParam("page", page)
                            .build(properties.owner()))
                    .header("Authorization", "Bearer " + properties.token())
                    .header("Accept", "application/vnd.github+json")
                    .retrieve()
                    .onStatus(status -> status.value() == 401, (req, res) -> {
                        throw new GitHubSyncException("GitHub 拒绝访问（HTTP 401）：PAT 无效或已过期");
                    })
                    .onStatus(status -> status.value() == 403, (req, res) -> {
                        throw new GitHubSyncException("GitHub 拒绝访问（HTTP 403）：PAT 权限不足或触发限流");
                    })
                    .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> {
                        throw new GitHubSyncException("GitHub API 异常响应（HTTP " + res.getStatusCode().value() + "）");
                    })
                    .body(new ParameterizedTypeReference<List<GitHubRepo>>() {
                    });
        } catch (GitHubSyncException e) {
            throw e;
        } catch (Exception e) {
            throw new GitHubSyncException("GitHub API 请求失败（网络异常或服务不可用）: " + e.getMessage(), e);
        }
    }
}
