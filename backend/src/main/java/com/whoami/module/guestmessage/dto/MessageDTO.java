package com.whoami.module.guestmessage.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

/**
 * 公开留言 DTO（GET /api/messages，仅 approved）。
 * 红线：绝不含 email 与 IP；content 纯文本，前端输出转义防 XSS。
 * ignoreUnknown=true：老客户端新增字段也不至于序列化失败，纯防御。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MessageDTO(
        Long id,
        String nickname,
        String content,
        String reply,
        LocalDateTime repliedAt,
        LocalDateTime createdAt) {
}
