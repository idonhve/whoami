package com.whoami.module.certificate.dto;

import java.time.LocalDate;

/**
 * 证书对外视图（GET /api/certificates）。
 * thumbUrl/imageUrl 为 /uploads/** 静态资源相对地址，前台按需加载缩略图与原图。
 */
public record CertificateDTO(
        Long id,
        String name,
        LocalDate obtainedAt,
        String thumbUrl,
        String imageUrl,
        int sortOrder) {
}