package com.whoami.module.siteconfig.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.whoami.common.BizException;
import com.whoami.module.siteconfig.dto.PublicSiteConfig;
import com.whoami.module.siteconfig.dto.SiteConfigDTO;
import com.whoami.module.siteconfig.entity.SiteConfig;
import com.whoami.module.siteconfig.mapper.SiteConfigMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class SiteConfigService {

    /**
     * 公开白名单键（初始登记，docs/spec/06-admin-cms.md）：
     * 只有这里的键允许通过 GET /api/site-config 下发前台，其余键一律只限后台。
     * 新增公开键 = 在此登记 + 在 PublicSiteConfig 增加字段 + 更新 spec 白名单说明。
     */
    public static final Set<String> PUBLIC_KEYS = Set.of(
            "domain", "owner_name", "github_url", "degrade_force_full");

    private final SiteConfigMapper siteConfigMapper;

    public SiteConfigService(SiteConfigMapper siteConfigMapper) {
        this.siteConfigMapper = siteConfigMapper;
    }

    /** 后台全量列表（含敏感键，仅 JWT 可访问） */
    public List<SiteConfigDTO> listAll() {
        List<SiteConfig> entities = siteConfigMapper.selectList(
                new LambdaQueryWrapper<SiteConfig>().orderByAsc(SiteConfig::getConfigKey));
        return entities.stream().map(this::toDTO).toList();
    }

    /** 公开白名单读取：只组装白名单键，敏感键即使存在也不会出现在响应里 */
    public PublicSiteConfig publicConfig() {
        // 注意：用字符串列名的 QueryWrapper（不用 Lambda），保证脱离 Spring 上下文也可单测
        List<SiteConfig> entities = siteConfigMapper.selectList(
                new QueryWrapper<SiteConfig>().in("config_key", PUBLIC_KEYS));
        Map<String, String> values = entities.stream()
                .collect(Collectors.toMap(SiteConfig::getConfigKey, e -> e.getConfigValue() == null ? "" : e.getConfigValue()));
        return new PublicSiteConfig(
                values.getOrDefault("domain", ""),
                values.getOrDefault("owner_name", ""),
                values.getOrDefault("github_url", ""),
                Boolean.parseBoolean(values.getOrDefault("degrade_force_full", "false")));
    }

    /** 更新已有键；键不存在返回 404（配置键由迁移脚本管理，不开放新建） */
    public void update(String key, String value, long adminId) {
        SiteConfig existing = siteConfigMapper.selectOne(
                new LambdaQueryWrapper<SiteConfig>().eq(SiteConfig::getConfigKey, key));
        if (existing == null) {
            throw new BizException(404, "配置键不存在: " + key);
        }
        SiteConfig update = new SiteConfig();
        update.setId(existing.getId());
        update.setConfigValue(value);
        update.setUpdatedBy(adminId);
        siteConfigMapper.updateById(update);
    }

    private SiteConfigDTO toDTO(SiteConfig entity) {
        return new SiteConfigDTO(
                entity.getConfigKey(), entity.getConfigValue(), entity.getDescription(), entity.getUpdatedAt());
    }

    /** 供按 key 精确查询（白名单断言等场景） */
    public SiteConfigDTO findByKey(String key) {
        SiteConfig entity = siteConfigMapper.selectOne(
                new LambdaQueryWrapper<SiteConfig>().eq(SiteConfig::getConfigKey, key));
        return entity == null ? null : toDTO(entity);
    }
}
