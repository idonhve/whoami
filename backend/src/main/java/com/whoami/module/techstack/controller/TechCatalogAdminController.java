package com.whoami.module.techstack.controller;

import com.whoami.common.ApiResult;
import com.whoami.module.techstack.dto.IdResult;
import com.whoami.module.techstack.dto.TechCatalogItem;
import com.whoami.module.techstack.dto.TechItem;
import com.whoami.module.techstack.service.TechStackService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 后台可选技术目录与自定义技术图标上传。 */
@Validated
@RestController
@RequestMapping("/admin/api/tech-catalog")
public class TechCatalogAdminController {

    private final TechStackService techStackService;

    public TechCatalogAdminController(TechStackService techStackService) {
        this.techStackService = techStackService;
    }

    @GetMapping
    public ApiResult<List<TechCatalogItem>> list() {
        return ApiResult.ok(techStackService.listCatalog());
    }

    @PostMapping("/custom")
    public ApiResult<IdResult> createCustom(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") @NotBlank @Size(max = 50) String name,
            @RequestParam("category") @NotBlank @Size(max = 20) String category,
            @RequestParam("proficiency") @Pattern(regexp = "master|proficient|familiar") String proficiency,
            @RequestParam("weight") @Min(1) @Max(100) Integer weight,
            @RequestParam(value = "sortOrder", required = false) Integer sortOrder) {
        TechItem created = techStackService.createCustomItem(file, name, category, proficiency, weight, sortOrder);
        return ApiResult.ok(new IdResult(created.id()));
    }
}
