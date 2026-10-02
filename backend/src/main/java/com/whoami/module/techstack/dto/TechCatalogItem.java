package com.whoami.module.techstack.dto;

/** 技术目录条目及当前是否已加入公开技术栈。 */
public record TechCatalogItem(
        Long id,
        String name,
        String icon,
        String iconUrl,
        String category,
        boolean custom,
        boolean inStack,
        Integer sortOrder) {
}
