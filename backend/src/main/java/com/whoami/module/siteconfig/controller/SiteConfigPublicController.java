package com.whoami.module.siteconfig.controller;

import com.whoami.common.ApiResult;
import com.whoami.module.siteconfig.dto.PublicSiteConfig;
import com.whoami.module.siteconfig.service.SiteConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开站点配置接口（免登录，JWT 过滤器只挂 /admin/api/*，不影响本接口）。
 * 只下发白名单键，敏感键永不下发。
 */
@RestController
@RequestMapping("/api/site-config")
public class SiteConfigPublicController {

    private final SiteConfigService siteConfigService;

    public SiteConfigPublicController(SiteConfigService siteConfigService) {
        this.siteConfigService = siteConfigService;
    }

    @GetMapping
    public ApiResult<PublicSiteConfig> publicConfig() {
        return ApiResult.ok(siteConfigService.publicConfig());
    }
}
