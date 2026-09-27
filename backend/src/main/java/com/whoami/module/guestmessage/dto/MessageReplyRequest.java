package com.whoami.module.guestmessage.dto;

import jakarta.validation.constraints.Size;

/** 管理员回复（PUT /admin/api/messages/{id}/reply）：reply 可空（空 = 清除回复，replied_at 一并清空）。 */
public record MessageReplyRequest(
        @Size(max = 500, message = "reply 不能超过 500 字符")
        String reply) {
}
