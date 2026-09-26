package com.whoami.module.project.controller;

import com.whoami.common.ApiResult;
import com.whoami.module.project.dto.ProjectCardDTO;
import com.whoami.module.project.service.ProjectService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 公开作品接口（免登录）：前台 /works 页与首页"精选作品"区共用，永不直连 GitHub（ADR-0001） */
@RestController
@RequestMapping("/api/projects")
public class ProjectPublicController {

    private final ProjectService projectService;

    public ProjectPublicController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ApiResult<List<ProjectCardDTO>> list(@RequestParam(defaultValue = "all") String scope) {
        return ApiResult.ok(projectService.listPublic(scope));
    }
}
