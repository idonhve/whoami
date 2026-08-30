package com.whoami.module.siteconfig.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * PUT /admin/api/site-config/{key} 请求体。
 * value 允许空字符串（如 github_url 暂未填写），但不允许缺字段。
 */
public record UpdateSiteConfigRequest(
        @NotNull(message = "value 不能为空") @Size(max = 65535, message = "value 过长") String value) {
}
