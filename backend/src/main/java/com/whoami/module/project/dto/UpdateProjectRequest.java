package com.whoami.module.project.dto;

import jakarta.validation.constraints.Size;

/**
 * 运营字段更新（PUT /admin/api/projects/{id}）。
 * 字段为 null 表示不修改；cnTitle 传空串表示清空中文描述（前台回退仓库 description）。
 */
public record UpdateProjectRequest(
        @Size(max = 200, message = "中文描述长度不能超过 200") String cnTitle,
        Boolean isPinned,
        Boolean isHidden,
        Integer sortOrder) {
}
