package com.whoami.module.certificate.dto;

import java.time.LocalDate;

/**
 * 证书运营字段更新（PUT /admin/api/certificates/{id}）。
 * 字段为 null 表示不修改；图片本身不可替换，换图请删除后重新上传。
 */
public record UpdateCertificateRequest(
        String name,
        LocalDate obtainedAt,
        Integer sortOrder) {
}