package com.whoami.module.experience.controller;

import com.whoami.common.ApiResult;
import com.whoami.module.experience.dto.ExperienceCreate;
import com.whoami.module.experience.dto.ExperienceDTO;
import com.whoami.module.experience.dto.IdResult;
import com.whoami.module.experience.service.ExperienceService;
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

@RestController
@RequestMapping("/admin/api/experiences")
public class ExperienceAdminController {

    private final ExperienceService experienceService;

    public ExperienceAdminController(ExperienceService experienceService) {
        this.experienceService = experienceService;
    }

    @GetMapping
    public ApiResult<List<ExperienceDTO>> listAll() {
        return ApiResult.ok(experienceService.listAll());
    }

    @PostMapping
    public ApiResult<IdResult> create(@Valid @RequestBody ExperienceCreate request) {
        return ApiResult.ok(experienceService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResult<Void> update(
            @PathVariable("id") long id,
            @Valid @RequestBody ExperienceCreate request) {
        experienceService.update(id, request);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable("id") long id) {
        experienceService.delete(id);
        return ApiResult.ok();
    }
}