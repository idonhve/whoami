package com.whoami.module.siteconfig.controller;

import com.whoami.common.ApiResult;
import com.whoami.module.siteconfig.dto.SiteConfigDTO;
import com.whoami.module.siteconfig.dto.UpdateSiteConfigRequest;
import com.whoami.module.siteconfig.service.SiteConfigService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/site-config")
public class SiteConfigAdminController {

    private final SiteConfigService siteConfigService;

    public SiteConfigAdminController(SiteConfigService siteConfigService) {
        this.siteConfigService = siteConfigService;
    }

    @GetMapping
    public ApiResult<List<SiteConfigDTO>> listAll() {
        return ApiResult.ok(siteConfigService.listAll());
    }

    @PutMapping("/{key}")
    public ApiResult<Void> update(
            @PathVariable("key") String key,
            @Valid @RequestBody UpdateSiteConfigRequest request,
            @RequestAttribute("adminId") long adminId) {
        siteConfigService.update(key, request.value(), adminId);
        return ApiResult.ok();
    }
}
