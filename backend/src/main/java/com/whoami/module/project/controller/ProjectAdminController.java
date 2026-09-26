package com.whoami.module.project.controller;

import com.whoami.common.ApiResult;
import com.whoami.module.github.dto.SyncLogDTO;
import com.whoami.module.github.dto.SyncResult;
import com.whoami.module.github.SyncService;
import com.whoami.module.github.mapper.SyncTaskLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.whoami.module.github.entity.SyncTaskLog;
import com.whoami.module.project.dto.ProjectAdminDTO;
import com.whoami.module.project.dto.UpdateProjectRequest;
import com.whoami.module.project.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台作品管理接口（JWT 保护，操作日志由 OpLogAspect 自动记录）。
 * 同步失败不抛 500：200 包络内 status=failed + 原因，前台继续展示库内缓存数据。
 */
@RestController
@RequestMapping("/admin/api/projects")
public class ProjectAdminController {

    private static final int MAX_LOG_LIMIT = 100;

    private final ProjectService projectService;
    private final SyncService syncService;
    private final SyncTaskLogMapper syncTaskLogMapper;

    public ProjectAdminController(ProjectService projectService, SyncService syncService,
                                  SyncTaskLogMapper syncTaskLogMapper) {
        this.projectService = projectService;
        this.syncService = syncService;
        this.syncTaskLogMapper = syncTaskLogMapper;
    }

    @GetMapping
    public ApiResult<List<ProjectAdminDTO>> list(
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Boolean pinned,
            @RequestParam(required = false) Boolean hidden) {
        return ApiResult.ok(projectService.listAdmin(language, pinned, hidden));
    }

    @PutMapping("/{id}")
    public ApiResult<Void> update(
            @PathVariable("id") long id,
            @Valid @RequestBody UpdateProjectRequest request,
            @RequestAttribute("adminId") long adminId) {
        projectService.update(id, request);
        return ApiResult.ok();
    }

    /** 手动同步（同步执行，HTTP 超时上限 60s；结果含失败原因） */
    @PostMapping("/sync")
    public ApiResult<SyncResult> sync() {
        return ApiResult.ok(syncService.syncNow("manual"));
    }

    @GetMapping("/sync/logs")
    public ApiResult<List<SyncLogDTO>> syncLogs(@RequestParam(defaultValue = "20") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_LOG_LIMIT);
        List<SyncLogDTO> logs = syncTaskLogMapper.selectList(
                        new LambdaQueryWrapper<SyncTaskLog>()
                                .orderByDesc(SyncTaskLog::getId)
                                .last("LIMIT " + safeLimit))
                .stream()
                .map(this::toDTO)
                .toList();
        return ApiResult.ok(logs);
    }

    private SyncLogDTO toDTO(SyncTaskLog entity) {
        return new SyncLogDTO(
                entity.getId(),
                entity.getTriggerType(),
                entity.getStatus(),
                entity.getRepoCount() == null ? 0 : entity.getRepoCount(),
                entity.getHiddenGone() == null ? 0 : entity.getHiddenGone(),
                entity.getMessage(),
                entity.getStartedAt(),
                entity.getFinishedAt());
    }
}
