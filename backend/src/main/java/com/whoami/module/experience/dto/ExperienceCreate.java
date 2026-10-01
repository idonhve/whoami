package com.whoami.module.experience.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * 经历卡创建/更新请求（POST 与 PUT 共用，契约见 docs/spec/09-experience.md）。
 * company/title/startDate 必填；endDate 可空（null = 至今），且不得早于 startDate（service 层跨字段校验）。
 * 公司介绍/项目介绍为自由文本；techTags/highlights 缺省按空数组处理。
 */
public record ExperienceCreate(
        @NotBlank(message = "company 不能为空")
        @Size(max = 50, message = "company 长度不能超过 50")
        String company,

        @NotBlank(message = "title 不能为空")
        @Size(max = 50, message = "title 长度不能超过 50")
        String title,

        @NotNull(message = "startDate 不能为空")
        LocalDate startDate,

        LocalDate endDate,

        String companyIntro,

        String projectIntro,

        @Size(max = 12, message = "techTags 最多 12 个")
        List<@Size(max = 30, message = "单个 techTag 最长 30") String> techTags,

        @Size(max = 10, message = "highlights 最多 10 条")
        List<String> highlights,

        Integer sortOrder) {

    /** Keeps existing Java callers source-compatible while the API form migrates to intro text. */
    @Deprecated
    public ExperienceCreate(
            String company,
            String title,
            LocalDate startDate,
            LocalDate endDate,
            List<AchievementItem> achievements,
            List<RadarItem> radar,
            List<String> techTags,
            List<String> highlights,
            Integer sortOrder) {
        this(company, title, startDate, endDate, (String) null, (String) null, techTags, highlights, sortOrder);
    }
}
