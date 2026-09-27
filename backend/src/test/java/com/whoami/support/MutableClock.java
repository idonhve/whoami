package com.whoami.support;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** 可拨动的测试时钟：注入 Service 后可在测试中前进时间（Spec 05 时钟可注入约束）。 */
public final class MutableClock extends Clock {

    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private volatile Instant instant;

    public MutableClock(Instant instant) {
        this.instant = instant;
    }

    public static MutableClock of(LocalDateTime wallTime) {
        return new MutableClock(wallTime.atZone(ZONE).toInstant());
    }

    public void advanceSeconds(long seconds) {
        instant = instant.plusSeconds(seconds);
    }

    @Override
    public ZoneId getZone() {
        return ZONE;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
