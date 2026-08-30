package com.whoami.module.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.whoami.common.BizException;
import com.whoami.module.project.dto.ProjectAdminDTO;
import com.whoami.module.project.dto.ProjectCardDTO;
import com.whoami.module.project.dto.UpdateProjectRequest;
import com.whoami.module.project.entity.Project;
import com.whoami.module.project.mapper.ProjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectMapper projectMapper;

    @InjectMocks
    private ProjectService projectService;

    private Project project(long id, long repoId, String name, String cnTitle, String descEn,
                            boolean pinned, boolean hidden, int sortOrder) {
        Project entity = new Project();
        entity.setId(id);
        entity.setRepoId(repoId);
        entity.setRepoName(name);
        entity.setFullName("idonhve/" + name);
        entity.setCnTitle(cnTitle);
        entity.setDescriptionEn(descEn);
        entity.setLanguage("Java");
        entity.setStargazersCount(12);
        entity.setForksCount(3);
        entity.setHtmlUrl("https://github.com/idonhve/" + name);
        entity.setPushedAt(LocalDateTime.of(2026, 8, 25, 9, 0));
        entity.setIsPinned(pinned);
        entity.setIsHidden(hidden);
        entity.setSortOrder(sortOrder);
        entity.setLastSyncedAt(LocalDateTime.of(2026, 8, 26, 3, 0));
        return entity;
    }

    @Test
    void listPublicAllMapsFields() {
        when(projectMapper.selectList(any())).thenReturn(List.of(
                project(1, 101, "whoami", null, "site", true, false, 5),
                project(2, 102, "cli", "工具", null, false, false, 0)));

        List<ProjectCardDTO> result = projectService.listPublic("all");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).isPinned()).isTrue();
        assertThat(result.get(0).cnTitle()).isEqualTo("site");
        assertThat(result.get(1).cnTitle()).isEqualTo("工具");
        assertThat(result.get(1).htmlUrl()).isEqualTo("https://github.com/idonhve/cli");
        assertThat(result.get(1).stargazersCount()).isEqualTo(12);
        assertThat(result.get(1).forksCount()).isEqualTo(3);
        assertThat(result.get(1).pushedAt()).isEqualTo(LocalDateTime.of(2026, 8, 25, 9, 0));
    }

    @Test
    void cnTitleBlankFallsBackToDescriptionEn() {
        when(projectMapper.selectList(any())).thenReturn(List.of(
                project(1, 101, "whoami", "", "github description", false, false, 0)));

        List<ProjectCardDTO> result = projectService.listPublic("all");

        assertThat(result.get(0).cnTitle()).isEqualTo("github description");
    }

    @Test
    void listAdminContainsOperationalFields() {
        when(projectMapper.selectList(any())).thenReturn(List.of(
                project(1, 101, "whoami", "我的主页", "site", false, true, 0)));

        List<ProjectAdminDTO> result = projectService.listAdmin(null, null, null);

        assertThat(result.get(0).repoId()).isEqualTo(101L);
        assertThat(result.get(0).repoName()).isEqualTo("whoami");
        assertThat(result.get(0).cnTitle()).isEqualTo("我的主页");
        assertThat(result.get(0).descriptionEn()).isEqualTo("site");
        assertThat(result.get(0).isHidden()).isTrue();
        assertThat(result.get(0).lastSyncedAt()).isEqualTo(LocalDateTime.of(2026, 8, 26, 3, 0));
    }

    @Test
    void updateWritesOnlyProvidedOperationalFields() {
        Project existing = project(1, 101, "whoami", null, "site", false, false, 0);
        when(projectMapper.selectById(1L)).thenReturn(existing);

        projectService.update(1L, new UpdateProjectRequest("我的主页", true, null, null));

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectMapper).updateById(captor.capture());
        Project update = captor.getValue();
        assertThat(update.getCnTitle()).isEqualTo("我的主页");
        assertThat(update.getIsPinned()).isTrue();
        // 未提供的字段不动
        assertThat(update.getIsHidden()).isNull();
        assertThat(update.getSortOrder()).isNull();
        // 元数据永不被管理接口改写
        assertThat(update.getStargazersCount()).isNull();
    }

    @Test
    void updateUnknownProjectThrows404() {
        when(projectMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> projectService.update(99L, new UpdateProjectRequest("x", null, null, null)))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getStatus()).isEqualTo(404));
    }

    @Test
    void pinningFourthProjectThrows409() {
        Project existing = project(4, 104, "fourth", null, null, false, false, 0);
        when(projectMapper.selectById(4L)).thenReturn(existing);
        when(projectMapper.selectCount(any())).thenReturn(3L);

        assertThatThrownBy(() -> projectService.update(4L, new UpdateProjectRequest(null, true, null, null)))
                .isInstanceOf(BizException.class)
                .satisfies(e -> {
                    assertThat(((BizException) e).getStatus()).isEqualTo(409);
                    assertThat(e.getMessage()).contains("置顶数量已达上限 3");
                });
    }

    @Test
    void keepingAlreadyPinnedProjectDoesNotConsumePinQuota() {
        Project existing = project(1, 101, "whoami", null, null, true, false, 0);
        when(projectMapper.selectById(1L)).thenReturn(existing);

        projectService.update(1L, new UpdateProjectRequest("改描述", null, null, null));

        verify(projectMapper).updateById(any(Project.class));
    }

    @Test
    void unpinNeverHitsPinLimit() {
        Project existing = project(1, 101, "whoami", null, null, true, false, 0);
        when(projectMapper.selectById(1L)).thenReturn(existing);

        projectService.update(1L, new UpdateProjectRequest(null, false, null, null));

        verify(projectMapper).updateById(any(Project.class));
    }
}
