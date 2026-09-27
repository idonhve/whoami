package com.whoami.module.stats.controller;

import com.whoami.common.ApiResult;
import com.whoami.module.stats.dto.DailyStatDTO;
import com.whoami.module.stats.dto.GeoProvinceDTO;
import com.whoami.module.stats.dto.ReferrerStatDTO;
import com.whoami.module.stats.dto.TopPageDTO;
import com.whoami.module.stats.service.StatsService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 后台统计看板接口（JWT 保护）：按日 PV/UV 曲线、TOP 页面、来源分布、访客地图（与公开 geo 同源）。 */
@RestController
@RequestMapping("/admin/api/stats")
public class StatsAdminController {

    private static final int MAX_DAYS = 365;

    private final StatsService statsService;

    public StatsAdminController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/daily")
    public ApiResult<List<DailyStatDTO>> daily(@RequestParam(defaultValue = "30") int days) {
        return ApiResult.ok(statsService.daily(safeDays(days)));
    }

    @GetMapping("/top-pages")
    public ApiResult<List<TopPageDTO>> topPages(@RequestParam(defaultValue = "30") int days) {
        return ApiResult.ok(statsService.topPages(safeDays(days)));
    }

    @GetMapping("/referrers")
    public ApiResult<List<ReferrerStatDTO>> referrers(@RequestParam(defaultValue = "30") int days) {
        return ApiResult.ok(statsService.referrers(safeDays(days)));
    }

    @GetMapping("/geo")
    public ApiResult<List<GeoProvinceDTO>> geo() {
        return ApiResult.ok(statsService.geo());
    }

    private static int safeDays(int days) {
        return Math.min(Math.max(days, 1), MAX_DAYS);
    }
}
