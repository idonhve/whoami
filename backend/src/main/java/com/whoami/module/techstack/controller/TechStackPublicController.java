package com.whoami.module.techstack.controller;

import com.whoami.common.ApiResult;
import com.whoami.module.techstack.dto.TechItem;
import com.whoami.module.techstack.service.TechStackService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开技术栈接口（GET /api/tech-stack，免登录）。JWT 过滤器只挂 /admin/api/*，不影响本接口。
 */
@RestController
@RequestMapping("/api/tech-stack")
public class TechStackPublicController {

    private final TechStackService techStackService;

    public TechStackPublicController(TechStackService techStackService) {
        this.techStackService = techStackService;
    }

    @GetMapping
    public ApiResult<List<TechItem>> list() {
        return ApiResult.ok(techStackService.listPublic());
    }
}