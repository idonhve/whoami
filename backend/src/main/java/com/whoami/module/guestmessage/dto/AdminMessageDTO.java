package com.whoami.module.guestmessage.dto;

import java.time.LocalDateTime;

/**
 * 后台留言 DTO（GET /admin/api/messages，全量含状态）。
 * ip 已脱敏（IpMasker，如 1.2.*.*）；email 仅此处对站主可见。
 */
public record AdminMessageDTO(
        Long id,
        String nickname,
        String email,
        String content,
        String status,
        String reply,
        LocalDateTime repliedAt,
        String ip,
        LocalDateTime createdAt) {
}
