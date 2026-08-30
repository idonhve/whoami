package com.whoami.module.siteconfig.dto;

/**
 * 公开下发的站点配置（仅白名单键，敏感键永不下发）。
 * 契约见 docs/spec/06-admin-cms.md：GET /api/site-config 返回
 * {domain, ownerName, githubUrl, degradeForceFull}，字段名为 camelCase。
 * 存储统一为字符串，degradeForceFull 由服务端转型为 boolean 后下发。
 */
public record PublicSiteConfig(String domain, String ownerName, String githubUrl, boolean degradeForceFull) {
}
