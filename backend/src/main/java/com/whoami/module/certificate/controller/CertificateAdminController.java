package com.whoami.module.certificate.controller;

import com.whoami.common.ApiResult;
import com.whoami.module.certificate.dto.IdResult;
import com.whoami.module.certificate.dto.UpdateCertificateRequest;
import com.whoami.module.certificate.service.CertificateService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 后台证书管理（/admin/api/certificates，JWT 保护，未登录 401；操作日志由 OpLogAspect 自动记录）。
 * 上传即生成缩略图与压缩原图；删除时物理文件一并清理。
 */
@RestController
@RequestMapping("/admin/api/certificates")
public class CertificateAdminController {

    private final CertificateService certificateService;

    public CertificateAdminController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    /** multipart 上传：file + name + obtainedAt（缺参与校验失败一律 400） */
    @PostMapping
    public ApiResult<IdResult> create(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "obtainedAt", required = false) String obtainedAt) {
        return ApiResult.ok(new IdResult(certificateService.create(file, name, obtainedAt)));
    }

    @PutMapping("/{id}")
    public ApiResult<Void> update(
            @PathVariable("id") long id,
            @RequestBody UpdateCertificateRequest request) {
        certificateService.update(id, request);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable("id") long id) {
        certificateService.delete(id);
        return ApiResult.ok();
    }
}