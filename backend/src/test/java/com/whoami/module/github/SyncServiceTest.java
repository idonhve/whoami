package com.whoami.module.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.whoami.module.github.dto.GitHubRepo;
import com.whoami.module.github.dto.SyncResult;
import com.whoami.module.github.entity.SyncTaskLog;
import com.whoami.module.github.mapper.SyncTaskLogMapper;
import com.whoami.module.project.entity.Project;
import com.whoami.module.project.mapper.ProjectMapper;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

    @Mock
    private GitHubClient gitHubClient;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private SyncTaskLogMapper syncTaskLogMapper;

    @InjectMocks
    private SyncService syncService;

    private GitHubRepo repo(long repoId, String name, int stars, String description) {
        return new GitHubRepo(repoId, name, "idonhve/" + name, description, "Java", stars, 2,
                "https://github.com/idonhve/" + name, LocalDateTime.of(2026, 8, 20, 10, 0));
    }

    private Project stored(long repoId, String name, String cnTitle, boolean pinned, boolean hidden) {
        Project project = new Project();
        project.setId(repoId * 10);
        project.setRepoId(repoId);
        project.setRepoName(name);
        project.setFullName("idonhve/" + name);
        project.setCnTitle(cnTitle);
        project.setDescriptionEn("old description");
        project.setLanguage("Java");
        project.setStargazersCount(1);
        project.setForksCount(1);
        project.setHtmlUrl("https://github.com/idonhve/" + name);
        project.setPushedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        project.setIsPinned(pinned);
        project.setIsHidden(hidden);
        project.setSortOrder(pinned ? 5 : 0);
        return project;
    }

    @Test
    void firstSyncInsertsAllReposWithDefaults() {
        when(gitHubClient.fetchOwnerRepos()).thenReturn(List.of(repo(1L, "whoami", 10, "site"), repo(2L, "cli", 3, "tool")));
        when(projectMapper.selectOne(any())).thenReturn(null);
        when(projectMapper.selectList(any())).thenReturn(List.of());

        SyncResult result = syncService.syncNow("manual");

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.repoCount()).isEqualTo(2);
        assertThat(result.hiddenGone()).isZero();
        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        Project inserted = captor.getAllValues().get(0);
        assertThat(inserted.getRepoId()).isEqualTo(1L);
        assertThat(inserted.getIsPinned()).isFalse();
        assertThat(inserted.getIsHidden()).isFalse();
        assertThat(inserted.getSortOrder()).isZero();
        assertThat(inserted.getStargazersCount()).isEqualTo(10);
        verify(syncTaskLogMapper).insert(org.mockito.ArgumentMatchers.<SyncTaskLog>argThat(
                log -> "manual".equals(log.getTriggerType())
                        && "success".equals(log.getStatus()) && log.getRepoCount() == 2));
    }

    @Test
    void resyncUpdatesMetadataButKeepsOperationalFields() {
        when(gitHubClient.fetchOwnerRepos()).thenReturn(List.of(repo(7L, "whoami", 99, "new description")));
        Project existing = stored(7L, "whoami", "我的主页", true, false);
        when(projectMapper.selectOne(any())).thenReturn(existing);
        when(projectMapper.selectList(any())).thenReturn(List.of(existing));

        SyncResult result = syncService.syncNow("scheduled");

        assertThat(result.status()).isEqualTo("success");
        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectMapper).updateById(captor.capture());
        Project update = captor.getValue();
        // 元数据覆盖
        assertThat(update.getStargazersCount()).isEqualTo(99);
        assertThat(update.getDescriptionEn()).isEqualTo("new description");
        assertThat(update.getPushedAt()).isEqualTo(LocalDateTime.of(2026, 8, 20, 10, 0));
        assertThat(update.getLastSyncedAt()).isNotNull();
        // 运营字段保留：更新对象不携带这些字段（MyBatis-Plus 只更新非 null 字段）
        assertThat(update.getCnTitle()).isNull();
        assertThat(update.getIsPinned()).isNull();
        assertThat(update.getIsHidden()).isNull();
        assertThat(update.getSortOrder()).isNull();
    }

    @Test
    void goneRepoIsAutoHiddenAndCounted() {
        when(gitHubClient.fetchOwnerRepos()).thenReturn(List.of(repo(7L, "whoami", 10, "site")));
        Project kept = stored(7L, "whoami", null, false, false);
        Project gone = stored(8L, "old-repo", null, false, false);
        when(projectMapper.selectOne(any())).thenReturn(kept);
        when(projectMapper.selectList(any())).thenReturn(List.of(kept, gone));

        SyncResult result = syncService.syncNow("manual");

        assertThat(result.hiddenGone()).isEqualTo(1);
        // gone 的自动隐藏：只置 is_hidden=true，不删除
        verify(projectMapper).updateById(org.mockito.ArgumentMatchers.<Project>argThat(
                p -> p.getId().equals(gone.getId())
                        && Boolean.TRUE.equals(p.getIsHidden()) && p.getRepoName() == null));
        verify(projectMapper, never()).deleteById(any(java.io.Serializable.class));
        verify(syncTaskLogMapper).insert(org.mockito.ArgumentMatchers.<SyncTaskLog>argThat(
                log -> log.getHiddenGone() == 1));
    }

    @Test
    void alreadyHiddenGoneRepoIsNotCountedAgain() {
        when(gitHubClient.fetchOwnerRepos()).thenReturn(List.of(repo(7L, "whoami", 10, "site")));
        Project kept = stored(7L, "whoami", null, false, false);
        Project hiddenGone = stored(9L, "gone", null, false, true);
        when(projectMapper.selectOne(any())).thenReturn(kept);
        when(projectMapper.selectList(any())).thenReturn(List.of(kept, hiddenGone));

        SyncResult result = syncService.syncNow("manual");

        assertThat(result.hiddenGone()).isZero();
    }

    @Test
    void missingPatFailsSyncAndWritesLogWithReason() {
        when(gitHubClient.fetchOwnerRepos())
                .thenThrow(new GitHubSyncException("未配置 GITHUB_TOKEN（只需只读公开仓库 PAT），无法同步"));

        SyncResult result = syncService.syncNow("scheduled");

        assertThat(result.status()).isEqualTo("failed");
        assertThat(result.message()).contains("GITHUB_TOKEN");
        verify(syncTaskLogMapper).insert(org.mockito.ArgumentMatchers.<SyncTaskLog>argThat(
                log -> "failed".equals(log.getStatus())
                        && log.getMessage().contains("GITHUB_TOKEN")));
        verify(projectMapper, never()).insert(any(Project.class));
    }

    @Test
    void github401FailsSyncWithPatExpiredMessage() {
        when(gitHubClient.fetchOwnerRepos())
                .thenThrow(new GitHubSyncException("GitHub 拒绝访问（HTTP 401）：PAT 无效或已过期"));

        SyncResult result = syncService.syncNow("manual");

        assertThat(result.status()).isEqualTo("failed");
        assertThat(result.message()).contains("401");
        verify(syncTaskLogMapper).insert(org.mockito.ArgumentMatchers.<SyncTaskLog>argThat(
                log -> "failed".equals(log.getStatus())));
    }

    @Test
    void scheduledCronIsDailyAt3Am() throws Exception {
        Method method = SyncService.class.getMethod("scheduledSync");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);
        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).isEqualTo("0 0 3 * * ?");
    }

    @Test
    void logWriteFailureNeverBreaksSync() {
        when(gitHubClient.fetchOwnerRepos()).thenReturn(List.of(repo(1L, "whoami", 10, "site")));
        when(projectMapper.selectOne(any())).thenReturn(null);
        when(projectMapper.selectList(any())).thenReturn(List.of());
        when(syncTaskLogMapper.insert(any(SyncTaskLog.class))).thenThrow(new RuntimeException("db down"));

        SyncResult result = syncService.syncNow("manual");

        // 日志写失败只告警，同步结果照常返回
        assertThat(result.status()).isEqualTo("success");
    }
}
