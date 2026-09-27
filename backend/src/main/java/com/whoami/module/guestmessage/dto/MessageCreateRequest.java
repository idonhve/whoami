package com.whoami.module.guestmessage.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 留言提交（POST /api/messages）。
 * 校验失败 400：nickname 必填 ≤ 20；content 必填 ≤ 500；email 可空但须邮箱格式（≤ 100）。
 */
public record MessageCreateRequest(
        @NotBlank(message = "nickname 不能为空")
        @Size(max = 20, message = "nickname 不能超过 20 字符")
        String nickname,

        @NotBlank(message = "content 不能为空")
        @Size(max = 500, message = "content 不能超过 500 字符")
        String content,

        @Email(message = "email 格式不正确")
        @Size(max = 100, message = "email 不能超过 100 字符")
        String email) {
}
