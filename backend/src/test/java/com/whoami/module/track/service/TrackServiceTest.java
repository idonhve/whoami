package com.whoami.module.track.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.whoami.common.BizException;
import com.whoami.module.track.dto.SessionEndRequest;
import com.whoami.module.track.dto.SessionStartRequest;
import com.whoami.module.track.dto.TrackEventRequest;
import com.whoami.module.track.entity.TrackEvent;
import com.whoami.module.track.entity.VisitLog;
import com.whoami.module.track.mapper.TrackEventMapper;
import com.whoami.module.track.mapper.VisitLogMapper;
import com.whoami.module.track.service.IpRegionResolver.IpRegion;
import com.whoami.support.MutableClock;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** TrackService 单元测试：duration 计算（拨钟）、end 幂等、eventType 非枚举 400、start 幂等。 */
class TrackServiceTest {

    private static final LocalDateTime BASE = LocalDateTime.parse("2026-01-01T10:00:00");

    private final MutableClock clock = MutableClock.of(BASE);
    private final VisitLogMapper visitLogMapper = mock(VisitLogMapper.class);
    private final TrackEventMapper trackEventMapper = mock(TrackEventMapper.class);
    private final IpRegionResolver ipRegionResolver = mock(IpRegionResolver.class);
    private final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
    private final TrackService service =
            new TrackService(visitLogMapper, trackEventMapper, ipRegionResolver, clock);

    @BeforeEach
    void setUp() {
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("unit-test-agent");
        when(ipRegionResolver.resolve(any())).thenReturn(new IpRegion(null, null));
    }

    @Test
    void endComputesDurationFromInjectedClock() {
        VisitLog existing = new VisitLog();
        existing.setId(7L);
        existing.setSessionId("s-1");
        existing.setEntryTime(LocalDateTime.parse("2026-01-01T09:57:30"));
        when(visitLogMapper.selectOne(any())).thenReturn(existing);
        clock.advanceSeconds(150);

        service.endSession("s-1", new SessionEndRequest(null));

        ArgumentCaptor<VisitLog> captor = ArgumentCaptor.forClass(VisitLog.class);
        verify(visitLogMapper).updateById(captor.capture());
        assertThat(captor.getValue().getLeaveTime()).isEqualTo(LocalDateTime.parse("2026-01-01T10:02:30"));
        assertThat(captor.getValue().getDurationSeconds()).isEqualTo(300);
    }

    @Test
    void endIsIdempotentWhenLeaveTimeAlreadyPresent() {
        VisitLog existing = new VisitLog();
        existing.setId(7L);
        existing.setSessionId("s-1");
        existing.setEntryTime(LocalDateTime.parse("2026-01-01T09:57:30"));
        existing.setLeaveTime(LocalDateTime.parse("2026-01-01T09:59:00"));
        existing.setDurationSeconds(90);
        when(visitLogMapper.selectOne(any())).thenReturn(existing);
        clock.advanceSeconds(150);

        service.endSession("s-1", new SessionEndRequest("/about"));

        verify(visitLogMapper, never()).updateById(any(VisitLog.class));
        // 已结束的会话也不再补记离开页 PV
        verify(trackEventMapper, never()).insert(any(TrackEvent.class));
    }

    @Test
    void endIgnoresUnknownSession() {
        when(visitLogMapper.selectOne(any())).thenReturn(null);
        service.endSession("missing", new SessionEndRequest("/"));
        verify(visitLogMapper, never()).updateById(any(VisitLog.class));
    }

    @Test
    void eventRejectsUnknownTypeWith400() {
        assertThatThrownBy(() -> service.recordEvent(
                new TrackEventRequest("s-1", "hacker_event", "/", null), httpRequest))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("eventType");
    }

    @Test
    void eventWritesTrackEventRowWithResolvedIp() {
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("114.114.114.114");

        service.recordEvent(new TrackEventRequest("s-1", "page_view", "/about ", null), httpRequest);

        ArgumentCaptor<TrackEvent> captor = ArgumentCaptor.forClass(TrackEvent.class);
        verify(trackEventMapper).insert(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("page_view");
        assertThat(captor.getValue().getSessionId()).isEqualTo("s-1");
        assertThat(captor.getValue().getPagePath()).isEqualTo("/about");
        assertThat(captor.getValue().getIp()).isEqualTo("114.114.114.114");
        assertThat(captor.getValue().getCreatedAt()).isEqualTo(BASE);
    }

    @Test
    void startSessionFillsServerSideColumnsAndIgnoresDuplicate() {
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("114.114.114.114");

        service.startSession(new SessionStartRequest("s-2", "https://github.com/", "/about"), httpRequest);

        ArgumentCaptor<VisitLog> captor = ArgumentCaptor.forClass(VisitLog.class);
        verify(visitLogMapper).insert(captor.capture());
        VisitLog inserted = captor.getValue();
        assertThat(inserted.getIp()).isEqualTo("114.114.114.114");
        assertThat(inserted.getUserAgent()).isEqualTo("unit-test-agent");
        assertThat(inserted.getReferrer()).isEqualTo("https://github.com/");
        assertThat(inserted.getEntryPage()).isEqualTo("/about");
        assertThat(inserted.getEntryTime()).isEqualTo(BASE);
        assertThat(inserted.getVisitDate()).isEqualTo(BASE.toLocalDate());

        // 重复 sessionId（唯一键冲突）被吞掉，不向调用方抛错
        when(visitLogMapper.insert(any(VisitLog.class)))
                .thenThrow(new org.springframework.dao.DuplicateKeyException("uk_session_id"));
        service.startSession(new SessionStartRequest("s-2", null, null), httpRequest);
    }

    @Test
    void startSessionFallsBackEntryPageToRefererPathThenRoot() {
        when(httpRequest.getHeader("Referer")).thenReturn("https://example.com/some/entry?utm=x");
        service.startSession(new SessionStartRequest("s-3", null, null), httpRequest);
        ArgumentCaptor<VisitLog> captor = ArgumentCaptor.forClass(VisitLog.class);
        verify(visitLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getEntryPage()).isEqualTo("/some/entry");

        when(httpRequest.getHeader("Referer")).thenReturn(null);
        service.startSession(new SessionStartRequest("s-4", null, null), httpRequest);
        verify(visitLogMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertThat(captor.getValue().getEntryPage()).isEqualTo("/");
    }
}
