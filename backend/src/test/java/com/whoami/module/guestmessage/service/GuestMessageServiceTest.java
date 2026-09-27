package com.whoami.module.guestmessage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.whoami.common.BizException;
import com.whoami.module.guestmessage.dto.MessageCreateRequest;
import com.whoami.module.guestmessage.dto.MessageDTO;
import com.whoami.module.guestmessage.entity.GuestMessage;
import com.whoami.module.guestmessage.mapper.GuestMessageMapper;
import com.whoami.support.MutableClock;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** GuestMessageService 单元测试：限流（注入时钟）、默认 approved、email 规整。 */
class GuestMessageServiceTest {

    private static final LocalDateTime BASE = LocalDateTime.parse("2026-01-01T10:00:00");

    private final MutableClock clock = MutableClock.of(BASE);
    private final GuestMessageMapper guestMessageMapper = mock(GuestMessageMapper.class);
    private final GuestMessageService service = new GuestMessageService(guestMessageMapper, clock);

    @Test
    void createRejectsFourthMessageWithinOneMinuteWindow() {
        when(guestMessageMapper.selectCount(any())).thenReturn(3L);
        assertThatThrownBy(() -> service.create(
                new MessageCreateRequest("张三", "你好", null), "198.51.100.7"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("频繁");
        verify(guestMessageMapper, org.mockito.Mockito.never()).insert(any(GuestMessage.class));
    }

    @Test
    void createStoresApprovedByDefaultAndTrimsFields() {
        when(guestMessageMapper.selectCount(any())).thenReturn(0L);

        MessageDTO dto = service.create(
                new MessageCreateRequest(" 张三 ", "  你好站长  ", " a@example.com "), "198.51.100.7");

        ArgumentCaptor<GuestMessage> captor = ArgumentCaptor.forClass(GuestMessage.class);
        verify(guestMessageMapper).insert(captor.capture());
        GuestMessage inserted = captor.getValue();
        assertThat(inserted.getStatus()).isEqualTo("approved");
        assertThat(inserted.getNickname()).isEqualTo("张三");
        assertThat(inserted.getContent()).isEqualTo("你好站长");
        assertThat(inserted.getEmail()).isEqualTo("a@example.com");
        assertThat(inserted.getIp()).isEqualTo("198.51.100.7");
        assertThat(inserted.getCreatedAt()).isEqualTo(BASE);
        assertThat(dto.nickname()).isEqualTo("张三");
    }

    @Test
    void blankEmailNormalizedToNull() {
        when(guestMessageMapper.selectCount(any())).thenReturn(0L);
        service.create(new MessageCreateRequest("张三", "你好", "   "), "198.51.100.7");
        ArgumentCaptor<GuestMessage> captor = ArgumentCaptor.forClass(GuestMessage.class);
        verify(guestMessageMapper).insert(captor.capture());
        assertThat(captor.getValue().getEmail()).isNull();
    }

    @Test
    void rateLimitWindowSlidesWithInjectedClock() {
        // 窗口起点取注入时钟：now=BASE 计数 2 条 → 放行
        when(guestMessageMapper.selectCount(any())).thenReturn(2L);
        service.create(new MessageCreateRequest("张三", "第一条", null), "198.51.100.9");

        // 拨钟 61 秒：窗口起点后移到 BASE+1s，BASE 时刻落库的旧数据不再计入窗口
        clock.advanceSeconds(61);
        when(guestMessageMapper.selectCount(any())).thenReturn(0L);
        service.create(new MessageCreateRequest("张三", "第二条", null), "198.51.100.9");

        verify(guestMessageMapper, org.mockito.Mockito.times(2)).insert(any(GuestMessage.class));
    }
}
