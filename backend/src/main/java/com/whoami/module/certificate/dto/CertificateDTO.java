package com.whoami.module.certificate.dto;

import java.time.LocalDate;

/**
 * 证书对外视图（GET /api/certificates）。
 * thumbUrl/imageUrl 为 /uploads/** 相对地址，图片使用缩略图/原图，PDF 使用原文件预览。
 */
public record CertificateDTO(
        Long id,
        String name,
        LocalDate obtainedAt,
        String thumbUrl,
        String imageUrl,
        int sortOrder) {
}
