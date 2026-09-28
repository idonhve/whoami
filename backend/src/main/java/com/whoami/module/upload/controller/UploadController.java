package com.whoami.module.upload.controller;

import com.whoami.common.BizException;
import com.whoami.module.upload.service.UploadBlobService;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

/**
 * /uploads/** 只读访问（URL 契约与原静态映射保持一致）。
 * 内容存 DB（upload_blob），由本接口流式返回；文件名含 UUID、内容不变，长缓存。
 */
@RestController
public class UploadController {

    private static final String PREFIX = "/uploads/";
    private static final Duration CACHE_TTL = Duration.ofDays(7);

    private final UploadBlobService uploadBlobService;

    public UploadController(UploadBlobService uploadBlobService) {
        this.uploadBlobService = uploadBlobService;
    }

    @GetMapping("/uploads/**")
    public ResponseEntity<byte[]> serve(HttpServletRequest request) {
        String path = UriUtils.decode(
                request.getRequestURI().substring(PREFIX.length()), StandardCharsets.UTF_8);
        if (path.isBlank() || path.contains("..")) {
            throw new BizException(404, "文件不存在");
        }
        UploadBlobService.StoredBlob blob = uploadBlobService.load(path);
        if (blob == null) {
            throw new BizException(404, "文件不存在");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(blob.contentType()))
                .cacheControl(CacheControl.maxAge(CACHE_TTL).cachePublic())
                .body(blob.content());
    }
}
