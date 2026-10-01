package com.whoami.module.experience.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whoami.common.BizException;
import com.whoami.module.experience.dto.ExperienceCreate;
import com.whoami.module.experience.dto.ExperienceDTO;
import com.whoami.module.experience.dto.IdResult;
import com.whoami.module.experience.entity.Experience;
import com.whoami.module.experience.mapper.ExperienceMapper;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 工作经历（Spec 09）。公开列表按 sortOrder 升序、再按 startDate 倒序；
 * 标签与展开要点序列化为 JSON 列存储。
 */
@Service
public class ExperienceService {

    private final ExperienceMapper experienceMapper;
    private final ObjectMapper objectMapper;

    public ExperienceService(ExperienceMapper experienceMapper, ObjectMapper objectMapper) {
        this.experienceMapper = experienceMapper;
        this.objectMapper = objectMapper;
    }

    /** 公开与管理接口共用：按 sortOrder 升序、再按 startDate 倒序 */
    public List<ExperienceDTO> listAll() {
        List<Experience> entities = experienceMapper.selectList(
                new LambdaQueryWrapper<Experience>()
                        .orderByAsc(Experience::getSortOrder)
                        .orderByDesc(Experience::getStartDate));
        return entities.stream().map(this::toDTO).toList();
    }

    public IdResult create(ExperienceCreate req) {
        validateCrossField(req);
        Experience entity = toEntity(req);
        experienceMapper.insert(entity);
        return new IdResult(entity.getId());
    }

    public void update(long id, ExperienceCreate req) {
        validateCrossField(req);
        Experience existing = experienceMapper.selectById(id);
        if (existing == null) {
            throw new BizException(404, "经历不存在: " + id);
        }
        Experience update = toEntity(req);
        update.setId(id);
        // 保留旧版字段中的历史数据；当前 API 已由公司介绍/项目介绍取代它们。
        update.setAchievements(existing.getAchievements());
        update.setRadar(existing.getRadar());
        experienceMapper.updateById(update);
    }

    public void delete(long id) {
        Experience existing = experienceMapper.selectById(id);
        if (existing == null) {
            throw new BizException(404, "经历不存在: " + id);
        }
        experienceMapper.deleteById(id);
    }

    /** 跨字段校验：日期不倒置（其余约束由 Bean Validation 守门） */
    private void validateCrossField(ExperienceCreate req) {
        if (req.endDate() != null && req.startDate().isAfter(req.endDate())) {
            throw new BizException(400, "startDate 不能晚于 endDate");
        }
    }

    private Experience toEntity(ExperienceCreate req) {
        Experience e = new Experience();
        e.setCompany(req.company());
        e.setTitle(req.title());
        e.setStartDate(req.startDate());
        e.setEndDate(req.endDate());
        e.setCompanyIntro(req.companyIntro());
        e.setProjectIntro(req.projectIntro());
        e.setAchievements("[]");
        e.setRadar("[]");
        e.setTechTags(writeJson(req.techTags()));
        e.setHighlights(writeJson(req.highlights()));
        e.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
        return e;
    }

    private ExperienceDTO toDTO(Experience e) {
        return new ExperienceDTO(
                e.getId(),
                e.getCompany(),
                e.getTitle(),
                e.getStartDate(),
                e.getEndDate(),
                e.getCompanyIntro(),
                e.getProjectIntro(),
                readJsonList(e.getTechTags(), String.class),
                readJsonList(e.getHighlights(), String.class),
                e.getSortOrder());
    }

    private String writeJson(List<?> list) {
        try {
            return objectMapper.writeValueAsString(list == null ? List.of() : list);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("序列化 JSON 列失败", ex);
        }
    }

    private <T> List<T> readJsonList(String json, Class<T> elementType) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
            return objectMapper.readValue(json, listType);
        } catch (Exception ex) {
            throw new IllegalStateException("反序列化 JSON 列失败", ex);
        }
    }
}
