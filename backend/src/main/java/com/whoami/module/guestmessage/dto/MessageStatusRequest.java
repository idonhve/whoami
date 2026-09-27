package com.whoami.module.guestmessage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 留言上下架（PUT /admin/api/messages/{id}/status）：status 只允许 approved / hidden。 */
public record MessageStatusRequest(
        @NotBlank(message = "status 不能为空")
        @Pattern(regexp = "approved|hidden", message = "status 必须是 approved/hidden")
        String status) {
}
