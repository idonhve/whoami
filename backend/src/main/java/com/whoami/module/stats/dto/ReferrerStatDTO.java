package com.whoami.module.stats.dto;

/** 来源分布（GET /admin/api/stats/referrers）：窗口内 visit_log.referrer 聚合（直接访问不计）。 */
public record ReferrerStatDTO(String referrer, long count) {
}
