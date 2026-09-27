package com.whoami.module.resume.dto;

import java.time.LocalDateTime;

/** GET /admin/api/resumes 响应元素：倒序版本列表。 */
public record ResumeVersionDTO(
        long id, int versionNo, String displayName, long sizeBytes, boolean isCurrent, LocalDateTime uploadedAt) {
}
