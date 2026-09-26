package com.whoami.module.github.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 开启 @Scheduled（Spec 04 每日 03:00 GitHub 同步） */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
