package com.whoami.module.experience.dto;

import java.time.LocalDate;
import java.util.List;

/** 经历卡响应 DTO（公开与管理接口同构）。 */
public record ExperienceDTO(
        Long id,
        String company,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String companyIntro,
        String projectIntro,
        List<String> techTags,
        List<String> highlights,
        Integer sortOrder) {
}
