package com.whoami.module.track.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 统一事件入口（POST /api/track/event）：page_view 也走这里。
 * eventType 必须在 EventType 枚举内，否则 400（校验在 TrackService，以枚举为单一事实来源）。
 */
public record TrackEventRequest(
        @NotBlank(message = "sessionId 不能为空")
        @Size(max = 36, message = "sessionId 不能超过 36 字符")
        String sessionId,

        @NotBlank(message = "eventType 不能为空")
        @Size(max = 32, message = "eventType 不能超过 32 字符")
        String eventType,

        @Size(max = 200, message = "pagePath 不能超过 200 字符")
        String pagePath,

        /** 事件明细（任意 JSON 对象），原样以 JSON 文本入库 */
        JsonNode detail) {
}
