package com.whoami.module.experience.controller;

import com.whoami.common.ApiResult;
import com.whoami.module.experience.dto.ExperienceDTO;
import com.whoami.module.experience.service.ExperienceService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/experiences")
public class ExperiencePublicController {

    private final ExperienceService experienceService;

    public ExperiencePublicController(ExperienceService experienceService) {
        this.experienceService = experienceService;
    }

    @GetMapping
    public ApiResult<List<ExperienceDTO>> list() {
        return ApiResult.ok(experienceService.listAll());
    }
}