package com.whoami.module.stats.dto;

import java.util.List;

/**
 * 访客地图省级聚合（GET /api/visit-stats/geo 与 GET /admin/api/stats/geo 同源）：
 * province 为 ip2region 原文口径（如"广东省"，与 DataV GeoJSON 省份名一致）；
 * count = 该省会话数；cities 为访问量 TOP5 城市。
 * 红线：本结构绝不含任何 IP 信息。
 */
public record GeoProvinceDTO(String province, long count, List<GeoCityDTO> cities) {
}
