package com.whoami.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 站内统一可注入时钟（Spec 05 硬性约束）：
 * 会话停留时长计算、留言限流窗口等时间语义一律从 Clock 取值，测试可替换为可拨动的实现。
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
