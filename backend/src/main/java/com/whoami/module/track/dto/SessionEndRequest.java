package com.whoami.module.track.dto;

import jakarta.validation.constraints.Size;

/**
 * 会话结束上报（POST /api/track/session/{sessionId}/end，幂等：已结束则忽略）。
 * lastPagePath 用于离开页 PV 兜底补记：仅当该会话在此页尚无 page_view 事件时补一条，避免双计。
 */
public record SessionEndRequest(
        @Size(max = 200, message = "lastPagePath 不能超过 200 字符")
        String lastPagePath) {
}
