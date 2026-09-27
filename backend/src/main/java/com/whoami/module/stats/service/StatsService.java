package com.whoami.module.stats.service;

import com.whoami.module.stats.dto.DailyStatDTO;
import com.whoami.module.stats.dto.GeoCityDTO;
import com.whoami.module.stats.dto.GeoProvinceDTO;
import com.whoami.module.stats.dto.ReferrerStatDTO;
import com.whoami.module.stats.dto.TopPageDTO;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 统计聚合（Spec 05 后台看板 + 前台访客地图，公开与后台 geo 同源渲染）。
 * 聚合 SQL 走 JdbcTemplate（与 ResumeService 直写 track_event 同一先例，mapper 层只服务单表 CRUD）。
 * 口径：PV = page_view 事件数；UV = 窗口日去重 sessionId（与 PV 同源，跨天会话在活跃日各计 1）。
 * 窗口时间基准取注入 Clock（days 由 controller clamp）。
 */
@Service
public class StatsService {

    /** 与 module/track/service/EventType.PAGE_VIEW 同值；stats 模块不依赖 track 模块代码，仅共享表口径 */
    private static final String EVENT_PAGE_VIEW = "page_view";

    private static final int TOP_PAGES_LIMIT = 10;
    private static final int REFERRERS_LIMIT = 10;
    private static final int CITY_TOP_LIMIT = 5;
    private static final String UNKNOWN_CITY = "未知";

    private final JdbcTemplate jdbc;
    private final Clock clock;

    public StatsService(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** 按日 PV/UV：连续日期补零，方便前端画曲线。 */
    public List<DailyStatDTO> daily(int days) {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.minusDays(days - 1L);
        Map<LocalDate, long[]> byDate = new HashMap<>();
        jdbc.query(
                "SELECT DATE(created_at) AS d, COUNT(*) AS pv, COUNT(DISTINCT session_id) AS uv "
                        + "FROM track_event WHERE event_type = ? AND created_at >= ? "
                        + "GROUP BY DATE(created_at)",
                rs -> {
                    byDate.put(rs.getDate("d").toLocalDate(),
                            new long[]{rs.getLong("pv"), rs.getLong("uv")});
                },
                EVENT_PAGE_VIEW, Timestamp.valueOf(start.atStartOfDay()));
        List<DailyStatDTO> result = new ArrayList<>(days);
        for (LocalDate date = start; !date.isAfter(today); date = date.plusDays(1)) {
            long[] value = byDate.getOrDefault(date, new long[]{0, 0});
            result.add(new DailyStatDTO(date, value[0], value[1]));
        }
        return result;
    }

    /** TOP 页面（page_view 按 page_path 聚合，无 path 的不计）。 */
    public List<TopPageDTO> topPages(int days) {
        return jdbc.query(
                "SELECT page_path, COUNT(*) AS pv FROM track_event "
                        + "WHERE event_type = ? AND page_path IS NOT NULL AND created_at >= ? "
                        + "GROUP BY page_path ORDER BY pv DESC LIMIT " + TOP_PAGES_LIMIT,
                (rs, i) -> new TopPageDTO(rs.getString("page_path"), rs.getLong("pv")),
                EVENT_PAGE_VIEW, Timestamp.valueOf(windowStart(days)));
    }

    /** 来源分布（visit_log.referrer 聚合；空 referrer = 直接访问，不计入）。 */
    public List<ReferrerStatDTO> referrers(int days) {
        return jdbc.query(
                "SELECT referrer, COUNT(*) AS cnt FROM visit_log "
                        + "WHERE referrer IS NOT NULL AND referrer <> '' AND entry_time >= ? "
                        + "GROUP BY referrer ORDER BY cnt DESC LIMIT " + REFERRERS_LIMIT,
                (rs, i) -> new ReferrerStatDTO(rs.getString("referrer"), rs.getLong("cnt")),
                Timestamp.valueOf(windowStart(days)));
    }

    /**
     * 访客地图省级聚合 + 每省城市 TOP5（公开接口与后台同源）。
     * 仅统计国内省份（境外 / 未解析的 province 为 null，不进地图）；全量无时间窗口（spec 未定义窗口）。
     */
    public List<GeoProvinceDTO> geo() {
        record Row(String province, String city, long count) {
        }
        List<Row> rows = jdbc.query(
                "SELECT province, city, COUNT(*) AS cnt FROM visit_log "
                        + "WHERE province IS NOT NULL GROUP BY province, city",
                (rs, i) -> new Row(rs.getString("province"), rs.getString("city"), rs.getLong("cnt")));

        Map<String, Long> provinceCount = new HashMap<>();
        Map<String, List<GeoCityDTO>> citiesByProvince = new LinkedHashMap<>();
        for (Row row : rows) {
            provinceCount.merge(row.province(), row.count(), Long::sum);
            citiesByProvince.computeIfAbsent(row.province(), k -> new ArrayList<>())
                    .add(new GeoCityDTO(row.city() == null ? UNKNOWN_CITY : row.city(), row.count()));
        }
        Comparator<GeoCityDTO> byCountDesc =
                Comparator.comparingLong(GeoCityDTO::count).reversed();
        return provinceCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> new GeoProvinceDTO(
                        entry.getKey(),
                        entry.getValue(),
                        citiesByProvince.get(entry.getKey()).stream()
                                .sorted(byCountDesc)
                                .limit(CITY_TOP_LIMIT)
                                .toList()))
                .toList();
    }

    private LocalDateTime windowStart(int days) {
        return LocalDate.now(clock).minusDays(days - 1L).atStartOfDay();
    }
}
