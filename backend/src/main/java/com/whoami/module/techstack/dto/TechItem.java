package com.whoami.module.techstack.dto;

/**
 * 技术项公开/管理列表项（GET /api/tech-stack 与 GET /admin/api/tech-stack）。
 * 字段口径见 docs/spec/02-tech-stack.md。
 */
public record TechItem(
        Long id,
        String name,
        String icon,
        String iconUrl,
        Long catalogId,
        String category,
        String proficiency,
        Integer weight,
        Integer sortOrder) {

    /** Retain the original constructor shape for existing backend callers. */
    public TechItem(Long id, String name, String icon, String category,
                    String proficiency, Integer weight, Integer sortOrder) {
        this(id, name, icon, null, null, category, proficiency, weight, sortOrder);
    }
}
