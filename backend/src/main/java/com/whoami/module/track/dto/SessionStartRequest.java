package com.whoami.module.track.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 会话开始上报（POST /api/track/session）。
 * 契约基线 {sessionId, referrer?}；entryPage 为可选增强字段（前端 SDK 传当前路径更准确），
 * 缺省时后端按 Referer 头路径、"/" 兜底补齐 entry_page（Spec 05：后端补齐进入页）。
 */
public record SessionStartRequest(
        @NotBlank(message = "sessionId 不能为空")
        @Size(max = 36, message = "sessionId 不能超过 36 字符")
        String sessionId,

        @Size(max = 500, message = "referrer 不能超过 500 字符")
        String referrer,

        @Size(max = 200, message = "entryPage 不能超过 200 字符")
        String entryPage) {
}
