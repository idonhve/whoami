package com.whoami.module.stats.controller;

import com.whoami.common.ApiResult;
import com.whoami.module.stats.dto.GeoProvinceDTO;
import com.whoami.module.stats.service.StatsService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开访客地图接口（"关于本站"页用，免登录）：
 * 省级聚合 + 每省城市 TOP5，与后台 /admin/api/stats/geo 同源渲染；
 * 红线：响应绝不含任何 IP 信息（GeoProvinceDTO 结构层面即无 IP 字段）。
 */
@RestController
@RequestMapping("/api/visit-stats")
public class VisitStatsPublicController {

    private final StatsService statsService;

    public VisitStatsPublicController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/geo")
    public ApiResult<List<GeoProvinceDTO>> geo() {
        return ApiResult.ok(statsService.geo());
    }
}
