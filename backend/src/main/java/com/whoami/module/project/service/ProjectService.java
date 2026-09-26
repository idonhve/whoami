package com.whoami.module.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.whoami.common.BizException;
import com.whoami.module.project.dto.ProjectAdminDTO;
import com.whoami.module.project.dto.ProjectCardDTO;
import com.whoami.module.project.dto.UpdateProjectRequest;
import com.whoami.module.project.entity.Project;
import com.whoami.module.project.mapper.ProjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProjectService {

    /** 置顶上限（业务规则，超出返回 409） */
    public static final int PIN_LIMIT = 3;

    private final ProjectMapper projectMapper;

    public ProjectService(ProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
    }

    /**
     * 公开列表：scope=all 全部未隐藏；scope=featured 仅置顶且未隐藏（首页"精选作品"）。
     * 排序：置顶优先 → sortOrder（数值大在前）→ pushedAt 倒序。
     */
    public List<ProjectCardDTO> listPublic(String scope) {
        LambdaQueryWrapper<Project> qw = new LambdaQueryWrapper<Project>()
                .eq(Project::getIsHidden, false)
                .orderByDesc(Project::getIsPinned)
                .orderByDesc(Project::getSortOrder)
                .orderByDesc(Project::getPushedAt);
        if ("featured".equalsIgnoreCase(scope)) {
            qw.eq(Project::getIsPinned, true);
        }
        return projectMapper.selectList(qw).stream().map(this::toCardDTO).toList();
    }

    /** 后台全量列表（含隐藏），可按语言/置顶/隐藏筛选 */
    public List<ProjectAdminDTO> listAdmin(String language, Boolean pinned, Boolean hidden) {
        LambdaQueryWrapper<Project> qw = new LambdaQueryWrapper<Project>()
                .orderByDesc(Project::getIsPinned)
                .orderByDesc(Project::getSortOrder)
                .orderByDesc(Project::getPushedAt);
        if (language != null && !language.isBlank()) {
            qw.eq(Project::getLanguage, language);
        }
        if (pinned != null) {
            qw.eq(Project::getIsPinned, pinned);
        }
        if (hidden != null) {
            qw.eq(Project::getIsHidden, hidden);
        }
        return projectMapper.selectList(qw).stream().map(this::toAdminDTO).toList();
    }

    /** 更新运营字段（中文描述/置顶/隐藏/排序）；同步任务永不写这些字段 */
    public void update(long id, UpdateProjectRequest request) {
        Project existing = projectMapper.selectById(id);
        if (existing == null) {
            throw new BizException(404, "作品不存在: " + id);
        }
        if (Boolean.TRUE.equals(request.isPinned()) && !Boolean.TRUE.equals(existing.getIsPinned())) {
            Long pinnedCount = projectMapper.selectCount(new LambdaQueryWrapper<Project>()
                    .eq(Project::getIsPinned, true)
                    .ne(Project::getId, id));
            if (pinnedCount >= PIN_LIMIT) {
                throw new BizException(409, "置顶数量已达上限 " + PIN_LIMIT);
            }
        }
        Project update = new Project();
        update.setId(id);
        update.setCnTitle(request.cnTitle());
        update.setIsPinned(request.isPinned());
        update.setIsHidden(request.isHidden());
        update.setSortOrder(request.sortOrder());
        projectMapper.updateById(update);
    }

    private ProjectCardDTO toCardDTO(Project entity) {
        String title = entity.getCnTitle() == null || entity.getCnTitle().isBlank()
                ? entity.getDescriptionEn()
                : entity.getCnTitle();
        return new ProjectCardDTO(
                entity.getId(),
                title,
                entity.getLanguage(),
                entity.getStargazersCount() == null ? 0 : entity.getStargazersCount(),
                entity.getForksCount() == null ? 0 : entity.getForksCount(),
                entity.getHtmlUrl(),
                entity.getPushedAt(),
                Boolean.TRUE.equals(entity.getIsPinned()),
                entity.getSortOrder() == null ? 0 : entity.getSortOrder());
    }

    private ProjectAdminDTO toAdminDTO(Project entity) {
        return new ProjectAdminDTO(
                entity.getId(),
                entity.getRepoId(),
                entity.getRepoName(),
                entity.getFullName(),
                entity.getCnTitle(),
                entity.getDescriptionEn(),
                entity.getLanguage(),
                entity.getStargazersCount() == null ? 0 : entity.getStargazersCount(),
                entity.getForksCount() == null ? 0 : entity.getForksCount(),
                entity.getHtmlUrl(),
                entity.getPushedAt(),
                Boolean.TRUE.equals(entity.getIsPinned()),
                Boolean.TRUE.equals(entity.getIsHidden()),
                entity.getSortOrder() == null ? 0 : entity.getSortOrder(),
                entity.getLastSyncedAt());
    }
}
