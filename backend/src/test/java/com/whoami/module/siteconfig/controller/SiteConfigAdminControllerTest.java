package com.whoami.module.siteconfig.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.whoami.common.BizException;
import com.whoami.common.GlobalExceptionHandler;
import com.whoami.module.siteconfig.dto.SiteConfigDTO;
import com.whoami.module.siteconfig.service.SiteConfigService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class SiteConfigAdminControllerTest {

    @Mock
    private SiteConfigService siteConfigService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SiteConfigAdminController(siteConfigService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listAllReturnsUnifiedEnvelope() throws Exception {
        when(siteConfigService.listAll())
                .thenReturn(List.of(new SiteConfigDTO("domain", "whoami.dev", "域名", LocalDateTime.of(2026, 8, 29, 12, 0))));

        mockMvc.perform(get("/admin/api/site-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].key").value("domain"))
                .andExpect(jsonPath("$.data[0].value").value("whoami.dev"))
                .andExpect(jsonPath("$.data[0].description").value("域名"));
    }

    @Test
    void updateReturnsEmptyDataEnvelope() throws Exception {
        mockMvc.perform(put("/admin/api/site-config/domain")
                        .requestAttr("adminId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"whoami.dev\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void updateUnknownKeyReturns404Envelope() throws Exception {
        doThrow(new BizException(404, "配置键不存在: nope"))
                .when(siteConfigService)
                .update(eq("nope"), eq("v"), anyLong());

        mockMvc.perform(put("/admin/api/site-config/nope")
                        .requestAttr("adminId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"v\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("配置键不存在: nope"));
    }

    @Test
    void missingValueFieldReturns400() throws Exception {
        mockMvc.perform(put("/admin/api/site-config/domain")
                        .requestAttr("adminId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
