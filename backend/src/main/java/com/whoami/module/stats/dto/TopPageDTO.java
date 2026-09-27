package com.whoami.module.stats.dto;

/** TOP 页面（GET /admin/api/stats/top-pages）：窗口内 page_view 事件按 page_path 聚合。 */
public record TopPageDTO(String pagePath, long pv) {
}
