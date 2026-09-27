package com.whoami.module.resume.dto;

import java.time.LocalDateTime;

/** GET /api/resume/latest 响应：按钮显隐与文案。exists=false 当无任何版本。 */
public record ResumeLatestDTO(boolean exists, String displayName, LocalDateTime updatedAt) {
}
