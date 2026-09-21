package com.whoami.module.experience.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 战果条目：数字/文本值 + 一句话语境。 */
public record AchievementItem(
        @NotBlank(message = "achievement.value 不能为空")
        @Size(max = 20, message = "achievement.value 最长 20")
        String value,

        @Size(max = 50, message = "achievement.context 最长 50")
        String context) {
}