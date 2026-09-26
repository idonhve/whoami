package com.whoami.module.github.dto;

import java.time.LocalDateTime;

/** 同步日志项（GET /admin/api/projects/sync/logs） */
public record SyncLogDTO(
        Long id,
        String triggerType,
        String status,
        int repoCount,
        int hiddenGone,
        String message,
        LocalDateTime startedAt,
        LocalDateTime finishedAt) {
}
