package com.whoami.module.experience.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whoami.common.BizException;
import com.whoami.module.experience.dto.ExperienceCreate;
import com.whoami.module.experience.dto.RadarItem;
import com.whoami.module.experience.entity.Experience;
import com.whoami.module.experience.mapper.ExperienceMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Spec 09 服务层契约：跨字段/去重校验抛 400、JSON 列序列化、404 处理。 */
@ExtendWith(MockitoExtension.class)
class ExperienceServiceTest {

    @Mock
    private ExperienceMapper experienceMapper;

    private ExperienceService service;

    @BeforeEach
    void setUp() {
        service = new ExperienceService(experienceMapper, new ObjectMapper());
    }

    @Test
    void createWithInvertedDatesThrows400() {
        ExperienceCreate inverted = new ExperienceCreate(
                "A社", "工程师", LocalDate.of(2020, 1, 1), LocalDate.of(2019, 1, 1),
                List.of(), List.of(
                        new RadarItem("后端", 60),
                        new RadarItem("前端", 70),
                        new RadarItem("性能", 80)),
                List.of(), List.of(), 0);

        assertThatThrownBy(() -> service.create(inverted))
                .isInstanceOf(BizException.class)
                .hasMessage("startDate 不能晚于 endDate");
    }

    @Test
    void createWithDuplicateRadarDimensionThrows400() {
        ExperienceCreate req = new ExperienceCreate(
                "A社", "工程师", LocalDate.of(2020, 1, 1), null,
                List.of(), List.of(
                        new RadarItem("后端", 60),
                        new RadarItem("后端", 70),
                        new RadarItem("性能", 80)),
                List.of(), List.of(), 0);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BizException.class)
                .hasMessage("radar 维度名不能重复");
    }

    @Test
    void createSerializesJsonColumnsAndReturnsId() {
        when(experienceMapper.insert(any(Experience.class))).thenAnswer(inv -> {
            Experience e = inv.getArgument(0);
            e.setId(7L);
            return 1;
        });

        long id = service.create(validCreate()).id();

        assertThat(id).isEqualTo(7L);
        ArgumentCaptor<Experience> captor = ArgumentCaptor.forClass(Experience.class);
        verify(experienceMapper).insert(captor.capture());
        Experience saved = captor.getValue();
        assertThat(saved.getSortOrder()).isEqualTo(0);
        assertThat(saved.getRadar()).contains("\"dimension\":\"后端\"");
        assertThat(saved.getAchievements()).isEqualTo("[]");
    }

    @Test
    void updateNotFoundThrows404() {
        when(experienceMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.update(99L, validCreate()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("经历不存在");
    }

    @Test
    void deleteNotFoundThrows404() {
        when(experienceMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("经历不存在");
    }

    private ExperienceCreate validCreate() {
        return new ExperienceCreate(
                "A社", "工程师", LocalDate.of(2020, 1, 1), LocalDate.of(2022, 1, 1),
                List.of(), List.of(
                        new RadarItem("后端", 60),
                        new RadarItem("前端", 70),
                        new RadarItem("性能", 80)),
                List.of("Java"), List.of("要点一"), null);
    }
}