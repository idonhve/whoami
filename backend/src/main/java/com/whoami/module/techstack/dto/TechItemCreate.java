package com.whoami.module.techstack.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 新增 / 编辑技术项的请求体（POST/PUT /admin/api/tech-stack）。
 * 契约见 docs/spec/02-tech-stack.md：
 *   name 必填 ≤ 50；icon 可空；category 必填 ≤ 20；
 *   proficiency 三档枚举；weight 1~100（校验失败返回 400）。
 * sortOrder 由站主自定义，非必填（默认 0）。
 */
public record TechItemCreate(
        @NotBlank(message = "name 不能为空")
        @Size(max = 50, message = "name 不能超过 50 字符")
        String name,

        @Size(max = 50, message = "icon 不能超过 50 字符")
        String icon,

        @NotBlank(message = "category 不能为空")
        @Size(max = 20, message = "category 不能超过 20 字符")
        String category,

        @NotBlank(message = "proficiency 不能为空")
        @Pattern(regexp = "master|proficient|familiar", message = "proficiency 必须是 master/proficient/familiar")
        String proficiency,

        @NotNull(message = "weight 不能为空")
        @Min(value = 1, message = "weight 最小为 1")
        @Max(value = 100, message = "weight 最大为 100")
        Integer weight,

        Integer sortOrder) {
}