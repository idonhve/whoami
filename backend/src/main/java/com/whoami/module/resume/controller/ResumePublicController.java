package com.whoami.module.resume.controller;

import com.whoami.common.ApiResult;
import com.whoami.module.resume.dto.ResumeDownload;
import com.whoami.module.resume.dto.ResumeLatestDTO;
import com.whoami.module.resume.service.ResumeService;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 公开简历接口（免登录）：按钮显隐/文案 + 最新版流下载（下载埋点由服务端直写）。 */
@RestController
@RequestMapping("/api/resume")
public class ResumePublicController {

    private final ResumeService resumeService;

    public ResumePublicController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @GetMapping("/latest")
    public ApiResult<ResumeLatestDTO> latest() {
        return ApiResult.ok(resumeService.latest());
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download(HttpServletRequest request) {
        ResumeDownload target = resumeService.download(request);
        String disposition = ContentDisposition.attachment()
                .filename(target.displayName(), StandardCharsets.UTF_8)
                .build()
                .toString();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(new FileSystemResource(target.path()));
    }
}
