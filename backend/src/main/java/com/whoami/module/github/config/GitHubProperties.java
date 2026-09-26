package com.whoami.module.github.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * GitHub 同步配置（Spec 04）。
 * token/owner 来自 .env 的 GITHUB_TOKEN / GITHUB_OWNER，只注入后端，永不下发前端。
 * apiBase 默认公网 GitHub，测试可指向本地 mock 服务器。
 */
@ConfigurationProperties(prefix = "app.github")
public record GitHubProperties(String token, String owner, String apiBase) {

    public GitHubProperties {
        if (apiBase == null || apiBase.isBlank()) {
            apiBase = "https://api.github.com";
        }
    }

    public boolean tokenMissing() {
        return token == null || token.isBlank();
    }

    public boolean ownerMissing() {
        return owner == null || owner.isBlank();
    }
}
