package com.whoami.module.siteconfig.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.whoami.common.BizException;
import com.whoami.module.siteconfig.dto.PublicSiteConfig;
import com.whoami.module.siteconfig.dto.SiteConfigDTO;
import com.whoami.module.siteconfig.entity.SiteConfig;
import com.whoami.module.siteconfig.mapper.SiteConfigMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SiteConfigServiceTest {

    @Mock
    private SiteConfigMapper siteConfigMapper;

    @InjectMocks
    private SiteConfigService siteConfigService;

    private SiteConfig entity(String key, String value) {
        SiteConfig entity = new SiteConfig();
        entity.setId(1L);
        entity.setConfigKey(key);
        entity.setConfigValue(value);
        entity.setDescription("desc-" + key);
        entity.setUpdatedAt(LocalDateTime.of(2026, 8, 29, 12, 0));
        return entity;
    }

    @Test
    void listAllMapsEntitiesToContractDTO() {
        when(siteConfigMapper.selectList(any()))
                .thenReturn(List.of(entity("domain", "example.com"), entity("owner_name", "站主")));

        List<SiteConfigDTO> result = siteConfigService.listAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).key()).isEqualTo("domain");
        assertThat(result.get(0).value()).isEqualTo("example.com");
        assertThat(result.get(0).description()).isEqualTo("desc-domain");
        assertThat(result.get(0).updatedAt()).isEqualTo(LocalDateTime.of(2026, 8, 29, 12, 0));
    }

    @Test
    void publicConfigAssemblesCamelCaseFieldsAndParsesBoolean() {
        when(siteConfigMapper.selectList(any()))
                .thenReturn(List.of(
                        entity("domain", "whoami.dev"),
                        entity("owner_name", "朱其振"),
                        entity("github_url", "https://github.com/idonhve"),
                        entity("degrade_force_full", "true")));

        PublicSiteConfig config = siteConfigService.publicConfig();

        assertThat(config.domain()).isEqualTo("whoami.dev");
        assertThat(config.ownerName()).isEqualTo("朱其振");
        assertThat(config.githubUrl()).isEqualTo("https://github.com/idonhve");
        assertThat(config.degradeForceFull()).isTrue();
    }

    @Test
    void publicConfigDefaultsMissingKeysToEmptyAndFalse() {
        when(siteConfigMapper.selectList(any())).thenReturn(List.of());

        PublicSiteConfig config = siteConfigService.publicConfig();

        assertThat(config.domain()).isEmpty();
        assertThat(config.ownerName()).isEmpty();
        assertThat(config.githubUrl()).isEmpty();
        assertThat(config.degradeForceFull()).isFalse();
    }

    @Test
    void updateExistingKeyWritesValueAndOperator() {
        when(siteConfigMapper.selectOne(any())).thenReturn(entity("domain", "old"));

        siteConfigService.update("domain", "new.dev", 7L);

        ArgumentCaptor<SiteConfig> captor = ArgumentCaptor.forClass(SiteConfig.class);
        verify(siteConfigMapper).updateById(captor.capture());
        assertThat(captor.getValue().getConfigValue()).isEqualTo("new.dev");
        assertThat(captor.getValue().getUpdatedBy()).isEqualTo(7L);
    }

    @Test
    void updateUnknownKeyThrows404() {
        when(siteConfigMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> siteConfigService.update("nope", "v", 1L))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getStatus()).isEqualTo(404));
    }
}
