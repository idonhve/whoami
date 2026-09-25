package com.whoami.module.techstack.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.whoami.common.BizException;
import com.whoami.common.GlobalExceptionHandler;
import com.whoami.module.techstack.dto.TechItem;
import com.whoami.module.techstack.service.TechStackService;
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
class TechStackAdminControllerTest {

    @Mock
    private TechStackService techStackService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TechStackAdminController(techStackService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static final String VALID_BODY =
            "{\"name\":\"Vue 3\",\"icon\":\"vuejs\",\"category\":\"前端\","
                    + "\"proficiency\":\"master\",\"weight\":8,\"sortOrder\":1}";

    @Test
    void listReturnsOrderedItems() throws Exception {
        when(techStackService.listAll()).thenReturn(List.of(
                new TechItem(1L, "Vue 3", "vuejs", "前端", "master", 8, 1),
                new TechItem(2L, "MySQL", null, "数据库", "proficient", 5, 2)));

        mockMvc.perform(get("/admin/api/tech-stack"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].name").value("Vue 3"))
                .andExpect(jsonPath("$.data[1].category").value("数据库"));
    }

    @Test
    void createReturnsIdEnvelope() throws Exception {
        when(techStackService.create(any())).thenReturn(new TechItem(9L, "Vue 3", "vuejs", "前端", "master", 8, 1));

        mockMvc.perform(post("/admin/api/tech-stack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(9));
    }

    @Test
    void updateReturnsEmptyEnvelope() throws Exception {
        mockMvc.perform(put("/admin/api/tech-stack/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void deleteReturnsEmptyEnvelope() throws Exception {
        mockMvc.perform(delete("/admin/api/tech-stack/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void updateMissingItemReturns404() throws Exception {
        doThrow(new BizException(404, "技术项不存在: 999"))
                .when(techStackService)
                .update(eq(999L), any());

        mockMvc.perform(put("/admin/api/tech-stack/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("技术项不存在: 999"));
    }

    @Test
    void invalidProficiencyReturns400() throws Exception {
        mockMvc.perform(post("/admin/api/tech-stack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"category\":\"前端\","
                                + "\"proficiency\":\"expert\",\"weight\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void missingNameReturns400() throws Exception {
        mockMvc.perform(post("/admin/api/tech-stack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"前端\",\"proficiency\":\"master\",\"weight\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void weightOutOfRangeReturns400() throws Exception {
        mockMvc.perform(post("/admin/api/tech-stack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"category\":\"前端\","
                                + "\"proficiency\":\"master\",\"weight\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void categoryTooLongReturns400() throws Exception {
        String longCategory = "q".repeat(21);
        String body = "{\"name\":\"X\",\"category\":\"" + longCategory + "\","
                + "\"proficiency\":\"master\",\"weight\":1}";
        mockMvc.perform(post("/admin/api/tech-stack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}