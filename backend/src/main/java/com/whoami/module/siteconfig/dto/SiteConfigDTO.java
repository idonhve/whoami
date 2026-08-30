package com.whoami.module.siteconfig.dto;

import java.time.LocalDateTime;

/**
 * 后台全量配置项。契约见 docs/spec/06-admin-cms.md：
 * GET /admin/api/site-config 返回 [{key, value, description, updatedAt}]
 */
public record SiteConfigDTO(String key, String value, String description, LocalDateTime updatedAt) {
}
