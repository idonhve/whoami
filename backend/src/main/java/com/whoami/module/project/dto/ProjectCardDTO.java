package com.whoami.module.project.dto;

import java.time.LocalDateTime;

/** 公开作品卡片（GET /api/projects）。cnTitle 为空时已回退仓库原始 description。 */
public record ProjectCardDTO(
        Long id,
        String cnTitle,
        String language,
        int stargazersCount,
        int forksCount,
        String htmlUrl,
        LocalDateTime pushedAt,
        boolean isPinned,
        int sortOrder) {
}
