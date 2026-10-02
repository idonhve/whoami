package com.whoami.module.techstack.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** 从技术目录添加到公开技术栈时可选的熟练度、权重和排序。 */
public record AddCatalogTechRequest(
        @NotBlank
        @Pattern(regexp = "master|proficient|familiar")
        String proficiency,

        @NotNull @Min(1) @Max(100)
        Integer weight,

        Integer sortOrder) {
}
