package com.whoami.module.track.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.whoami.module.track.dto.SessionEndRequest;
import com.whoami.module.track.dto.SessionStartRequest;
import com.whoami.module.track.dto.TrackEventRequest;
import com.whoami.module.track.entity.TrackEvent;
import com.whoami.module.track.entity.VisitLog;
import com.whoami.module.track.mapper.TrackEventMapper;
import com.whoami.module.track.mapper.VisitLogMapper;
import com.whoami.module.track.service.IpRegionResolver.IpRegion;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 埋点上报（Spec 05）：会话开始 / 结束 + 统一事件入口。
 * 时钟可注入（duration 用 Clock 计算）；IP 只入库不外发；埋点写库失败不阻塞主流程的部分
 * 仅限 end 的兜底补记（start/event 失败即本次上报失败，由前端 SDK 自然重试语义兜底）。
 */
@Service
public class TrackService {

    private static final Logger log = LoggerFactory.getLogger(TrackService.class);

    /** 与 visit_log 列宽一致 */
    private static final int IP_MAX = 45;
    private static final int UA_MAX = 500;
    private static final int REFERRER_MAX = 500;
    private static final int PATH_MAX = 200;

    private final VisitLogMapper visitLogMapper;
    private final TrackEventMapper trackEventMapper;
    private final IpRegionResolver ipRegionResolver;
    private final Clock clock;

    public TrackService(VisitLogMapper visitLogMapper, TrackEventMapper trackEventMapper,
                        IpRegionResolver ipRegionResolver, Clock clock) {
        this.visitLogMapper = visitLogMapper;
        this.trackEventMapper = trackEventMapper;
        this.ipRegionResolver = ipRegionResolver;
        this.clock = clock;
    }

    /**
     * 会话开始：写 visit_log 一行，后端补齐 IP / 省市（ip2region）/ UA / entry_page / entry_time / visit_date。
     * sessionId 唯一冲突（重复上报）则忽略，保证"一次会话一行"。
     */
    public void startSession(SessionStartRequest request, HttpServletRequest httpRequest) {
        LocalDateTime now = LocalDateTime.now(clock);
        String ip = clientIp(httpRequest);

        VisitLog entry = new VisitLog();
        entry.setSessionId(request.sessionId());
        entry.setIp(truncate(ip, IP_MAX));
        IpRegion region = ipRegionResolver.resolve(ip);
        entry.setProvince(region.province());
        entry.setCity(region.city());
        entry.setUserAgent(truncate(httpRequest.getHeader("User-Agent"), UA_MAX));
        entry.setReferrer(truncateToNull(request.referrer(), REFERRER_MAX));
        entry.setEntryPage(resolveEntryPage(request, httpRequest));
        entry.setEntryTime(now);
        entry.setVisitDate(now.toLocalDate());
        try {
            visitLogMapper.insert(entry);
        } catch (DuplicateKeyException e) {
            // 同 sessionId 重复上报：忽略（幂等）
            log.debug("重复的 sessionId 上报，忽略: {}", request.sessionId());
        }
    }

    /**
     * 会话结束：写 leave_time，服务端按注入时钟计算 duration_seconds。
     * 幂等：会话不存在或已有 leave_time 均直接忽略。
     * lastPagePath 兜底：仅当该会话在此页尚无 page_view 事件时补一条（正常浏览下 SDK 已报过，避免双计 PV）。
     */
    public void endSession(String sessionId, SessionEndRequest request) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        VisitLog existing = visitLogMapper.selectOne(new LambdaQueryWrapper<VisitLog>()
                .eq(VisitLog::getSessionId, sessionId)
                .last("LIMIT 1"));
        if (existing == null || existing.getLeaveTime() != null) {
            return;
        }
        LocalDateTime leaveTime = LocalDateTime.now(clock);
        long seconds = Math.max(0, Duration.between(existing.getEntryTime(), leaveTime).getSeconds());

        VisitLog update = new VisitLog();
        update.setId(existing.getId());
        update.setLeaveTime(leaveTime);
        update.setDurationSeconds((int) Math.min(seconds, Integer.MAX_VALUE));
        visitLogMapper.updateById(update);

        backfillLastPageView(sessionId, request);
    }

    /**
     * 统一事件入口：eventType 必须在枚举内（否则 400）；detail 以 JSON 文本入库。
     * page_view 不做硬限流（Spec 05：正常浏览量级内可承受）。
     */
    public void recordEvent(TrackEventRequest request, HttpServletRequest httpRequest) {
        EventType type = EventType.parse(request.eventType());
        TrackEvent event = new TrackEvent();
        event.setEventType(type.value());
        event.setSessionId(request.sessionId());
        event.setPagePath(truncateToNull(request.pagePath(), PATH_MAX));
        event.setDetail(request.detail() == null ? null : request.detail().toString());
        event.setIp(truncate(clientIp(httpRequest), IP_MAX));
        event.setCreatedAt(LocalDateTime.now(clock));
        trackEventMapper.insert(event);
    }

    /** 离开页 PV 兜底：仅在该会话 + 该页尚无 page_view 事件时补记一条 */
    private void backfillLastPageView(String sessionId, SessionEndRequest request) {
        if (request == null || request.lastPagePath() == null || request.lastPagePath().isBlank()) {
            return;
        }
        String pagePath = truncateToNull(request.lastPagePath(), PATH_MAX);
        if (pagePath == null) {
            return;
        }
        Long existing = trackEventMapper.selectCount(new LambdaQueryWrapper<TrackEvent>()
                .eq(TrackEvent::getSessionId, sessionId)
                .eq(TrackEvent::getEventType, EventType.PAGE_VIEW.value())
                .eq(TrackEvent::getPagePath, pagePath));
        if (existing != null && existing > 0) {
            return;
        }
        TrackEvent event = new TrackEvent();
        event.setEventType(EventType.PAGE_VIEW.value());
        event.setSessionId(sessionId);
        event.setPagePath(pagePath);
        event.setDetail(null);
        event.setIp(visitLogIp(sessionId));
        event.setCreatedAt(LocalDateTime.now(clock));
        trackEventMapper.insert(event);
    }

    private String visitLogIp(String sessionId) {
        VisitLog visit = visitLogMapper.selectOne(new LambdaQueryWrapper<VisitLog>()
                .eq(VisitLog::getSessionId, sessionId)
                .last("LIMIT 1"));
        return visit == null ? null : visit.getIp();
    }

    /** entry_page 补齐优先级：请求体 entryPage → Referer 头路径 → "/" */
    private String resolveEntryPage(SessionStartRequest request, HttpServletRequest httpRequest) {
        if (request.entryPage() != null && !request.entryPage().isBlank()) {
            return truncate(request.entryPage().trim(), PATH_MAX);
        }
        String referer = httpRequest.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            try {
                String path = URI.create(referer.trim()).getPath();
                if (path != null && !path.isBlank()) {
                    return truncate(path, PATH_MAX);
                }
            } catch (IllegalArgumentException ignored) {
                // 非法 Referer：走默认值
            }
        }
        return "/";
    }

    /** 与 ResumeService / OpLogAspect 相同口径：X-Forwarded-For 首个优先（nginx 反代场景） */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String truncateToNull(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : (trimmed.length() <= max ? trimmed : trimmed.substring(0, max));
    }
}
