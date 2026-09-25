package com.whoami.module.techstack.dto;

/**
 * 技术项公开/管理列表项（GET /api/tech-stack 与 GET /admin/api/tech-stack）。
 * 字段口径见 docs/spec/02-tech-stack.md。
 */
public record TechItem(
        Long id,
        String name,
        String icon,
        String category,
        String proficiency,
        Integer weight,
        Integer sortOrder) {
}