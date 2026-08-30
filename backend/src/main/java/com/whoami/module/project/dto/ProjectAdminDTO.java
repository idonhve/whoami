package com.whoami.module.project.dto;

import java.time.LocalDateTime;

/** 后台作品列表项（GET /admin/api/projects，含 repo_id / is_hidden / last_synced_at） */
public record ProjectAdminDTO(
        Long id,
        Long repoId,
        String repoName,
        String fullName,
        String cnTitle,
        String descriptionEn,
        String language,
        int stargazersCount,
        int forksCount,
        String htmlUrl,
        LocalDateTime pushedAt,
        boolean isPinned,
        boolean isHidden,
        int sortOrder,
        LocalDateTime lastSyncedAt) {
}
