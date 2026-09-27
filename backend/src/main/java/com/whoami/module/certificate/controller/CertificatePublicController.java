package com.whoami.module.certificate.controller;

import com.whoami.common.ApiResult;
import com.whoami.module.certificate.dto.CertificateDTO;
import com.whoami.module.certificate.service.CertificateService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前台证书列表（/api/certificates，公开）。数据来自 certificate 表，后台增删改后即时生效。
 */
@RestController
@RequestMapping("/api/certificates")
public class CertificatePublicController {

    private final CertificateService certificateService;

    public CertificatePublicController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @GetMapping
    public ApiResult<List<CertificateDTO>> list() {
        return ApiResult.ok(certificateService.listPublic());
    }
}