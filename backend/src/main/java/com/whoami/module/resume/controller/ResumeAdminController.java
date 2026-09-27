package com.whoami.module.resume.controller;

import com.whoami.common.ApiResult;
import com.whoami.module.resume.dto.ResumeVersionDTO;
import com.whoami.module.resume.dto.UploadResultDTO;
import com.whoami.module.resume.service.ResumeService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 后台简历管理接口（JWT 保护，操作日志由 OpLogAspect 自动记录）。 */
@RestController
@RequestMapping("/admin/api/resumes")
public class ResumeAdminController {

    private final ResumeService resumeService;

    public ResumeAdminController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping
    public ApiResult<UploadResultDTO> upload(@RequestPart("file") MultipartFile file) {
        UploadResultDTO result = resumeService.upload(file);
        String message = result.evictedVersionNos().isEmpty()
                ? "ok"
                : "ok，已淘汰版本 " + result.evictedVersionNos();
        return new ApiResult<>(ApiResult.SUCCESS_CODE, message, result);
    }

    @GetMapping
    public ApiResult<List<ResumeVersionDTO>> list() {
        return ApiResult.ok(resumeService.listAdmin());
    }

    @PutMapping("/{id}/restore")
    public ApiResult<Void> restore(@PathVariable("id") long id) {
        resumeService.restore(id);
        return ApiResult.ok();
    }
}
