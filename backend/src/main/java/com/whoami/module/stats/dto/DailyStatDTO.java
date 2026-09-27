package com.whoami.module.stats.dto;

import java.time.LocalDate;

/** 按日统计（GET /admin/api/stats/daily）：PV = page_view 事件数；UV = 当日去重 sessionId。缺失日补零。 */
public record DailyStatDTO(LocalDate date, long pv, long uv) {
}
