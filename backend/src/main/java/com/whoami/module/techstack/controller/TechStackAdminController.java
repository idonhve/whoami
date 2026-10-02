package com.whoami.module.techstack.controller;

import com.whoami.common.ApiResult;
import com.whoami.module.techstack.dto.AddCatalogTechRequest;
import com.whoami.module.techstack.dto.IdResult;
import com.whoami.module.techstack.dto.TechItem;
import com.whoami.module.techstack.dto.TechItemCreate;
import com.whoami.module.techstack.service.TechStackService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台技术栈管理（/admin/api/tech-stack，JWT 保护，未登录 401）。
 * 增删改后公开接口 GET /api/tech-stack 即时反映，无需发版。
 */
@RestController
@RequestMapping("/admin/api/tech-stack")
public class TechStackAdminController {

    private final TechStackService techStackService;

    public TechStackAdminController(TechStackService techStackService) {
        this.techStackService = techStackService;
    }

    @GetMapping
    public ApiResult<List<TechItem>> listAll() {
        return ApiResult.ok(techStackService.listAll());
    }

    @PostMapping
    public ApiResult<IdResult> create(@Valid @RequestBody TechItemCreate request) {
        TechItem created = techStackService.create(request);
        return ApiResult.ok(new IdResult(created.id()));
    }

    @PostMapping("/catalog/{catalogId}")
    public ApiResult<IdResult> addCatalogItem(
            @PathVariable("catalogId") long catalogId,
            @Valid @RequestBody AddCatalogTechRequest request) {
        TechItem created = techStackService.addCatalogItem(catalogId, request);
        return ApiResult.ok(new IdResult(created.id()));
    }

    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable("id") long id, @Valid @RequestBody TechItemCreate request) {
        techStackService.update(id, request);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable("id") long id) {
        techStackService.delete(id);
        return ApiResult.ok();
    }
}
