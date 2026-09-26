package com.whoami.module.experience.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 雷达图维度：维度名 + 分值（0~100 整数）。维度名重复由 service 层校验。 */
public record RadarItem(
        @NotBlank(message = "radar.dimension 不能为空")
        @Size(max = 20, message = "radar.dimension 最长 20")
        String dimension,

        @Min(value = 0, message = "radar.score 应为 0~100 整数")
        @Max(value = 100, message = "radar.score 应为 0~100 整数")
        int score) {
}