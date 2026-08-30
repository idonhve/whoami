package com.whoami.module.github;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.whoami.module.github.dto.GitHubRepo;
import com.whoami.module.github.dto.SyncResult;
import com.whoami.module.github.entity.SyncTaskLog;
import com.whoami.module.github.mapper.SyncTaskLogMapper;
import com.whoami.module.project.entity.Project;
import com.whoami.module.project.mapper.ProjectMapper;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * GitHub 仓库同步（Spec 04，ADR-0001：后端定时同步入库，前台永不直连 GitHub）。
 *
 * upsert 语义：repo_id 为唯一键；仓库元数据覆盖更新；运营字段（中文描述/置顶/隐藏/排序）保留不覆盖；
 * GitHub 上已不存在的仓库不删除、自动置 is_hidden=true（防死链）并在日志计数 hidden_gone。
 * 仓库重新出现时不自动取消隐藏（运营字段归站主管），后台可手动恢复。
 * 每次同步（定时/手动）都写一条 sync_task_log；PAT 缺失/网络失败 → status=failed + 原因。
 */
@Service
public class SyncService {

    /** 每日 03:00（PRD §7.2 代决项，站主可推翻，改动只需调整此表达式） */
    public static final String SYNC_CRON = "0 0 3 * * ?";

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);
    private static final int LOG_MESSAGE_MAX = 500;

    private final GitHubClient gitHubClient;
    private final ProjectMapper projectMapper;
    private final SyncTaskLogMapper syncTaskLogMapper;

    public SyncService(GitHubClient gitHubClient, ProjectMapper projectMapper, SyncTaskLogMapper syncTaskLogMapper) {
        this.gitHubClient = gitHubClient;
        this.projectMapper = projectMapper;
        this.syncTaskLogMapper = syncTaskLogMapper;
    }

    @Scheduled(cron = SYNC_CRON, zone = "Asia/Shanghai")
    public void scheduledSync() {
        syncNow("scheduled");
    }

    /** 同步入口（scheduled 定时与 manual 手动共用，保证幂等语义一致） */
    public SyncResult syncNow(String triggerType) {
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            List<GitHubRepo> repos = gitHubClient.fetchOwnerRepos();
            SyncResult result = applyRepos(repos, startedAt);
            writeLog(triggerType, "success", result.repoCount(), result.hiddenGone(), null, startedAt);
            return result;
        } catch (Exception e) {
            String message = truncate(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            log.warn("GitHub 同步失败（{}）: {}", triggerType, message);
            writeLog(triggerType, "failed", 0, 0, message, startedAt);
            return SyncResult.failed(message);
        }
    }

    private SyncResult applyRepos(List<GitHubRepo> repos, LocalDateTime syncedAt) {
        for (GitHubRepo repo : repos) {
            Project existing = projectMapper.selectOne(
                    new LambdaQueryWrapper<Project>().eq(Project::getRepoId, repo.id()));
            if (existing == null) {
                projectMapper.insert(toNewProject(repo, syncedAt));
            } else {
                // 只带元数据字段：MyBatis-Plus 默认只更新非 null 字段，运营字段自然保留
                projectMapper.updateById(toMetadataUpdate(existing.getId(), repo, syncedAt));
            }
        }
        int hiddenGone = hideGoneRepos(repos);
        return SyncResult.success(repos.size(), hiddenGone);
    }

    private Project toNewProject(GitHubRepo repo, LocalDateTime syncedAt) {
        Project project = new Project();
        project.setRepoId(repo.id());
        project.setRepoName(repo.name());
        project.setFullName(repo.fullName());
        project.setDescriptionEn(repo.description());
        project.setLanguage(repo.language());
        project.setStargazersCount(repo.stargazersCount() == null ? 0 : repo.stargazersCount());
        project.setForksCount(repo.forksCount() == null ? 0 : repo.forksCount());
        project.setHtmlUrl(repo.htmlUrl());
        project.setPushedAt(repo.pushedAt());
        project.setIsPinned(false);
        project.setIsHidden(false);
        project.setSortOrder(0);
        project.setLastSyncedAt(syncedAt);
        return project;
    }

    private Project toMetadataUpdate(Long id, GitHubRepo repo, LocalDateTime syncedAt) {
        Project update = new Project();
        update.setId(id);
        update.setRepoName(repo.name());
        update.setFullName(repo.fullName());
        update.setDescriptionEn(repo.description());
        update.setLanguage(repo.language());
        update.setStargazersCount(repo.stargazersCount() == null ? 0 : repo.stargazersCount());
        update.setForksCount(repo.forksCount() == null ? 0 : repo.forksCount());
        update.setHtmlUrl(repo.htmlUrl());
        update.setPushedAt(repo.pushedAt());
        update.setLastSyncedAt(syncedAt);
        return update;
    }

    /** 库里存在但远端已消失的仓库：不删除，自动隐藏（防死链）；已隐藏的不重复计数 */
    private int hideGoneRepos(List<GitHubRepo> repos) {
        Set<Long> remoteIds = new HashSet<>();
        for (GitHubRepo repo : repos) {
            remoteIds.add(repo.id());
        }
        int hiddenGone = 0;
        for (Project project : projectMapper.selectList(null)) {
            if (!remoteIds.contains(project.getRepoId()) && !Boolean.TRUE.equals(project.getIsHidden())) {
                Project update = new Project();
                update.setId(project.getId());
                update.setIsHidden(true);
                projectMapper.updateById(update);
                hiddenGone++;
            }
        }
        return hiddenGone;
    }

    private void writeLog(String triggerType, String status, int repoCount, int hiddenGone,
                          String message, LocalDateTime startedAt) {
        try {
            SyncTaskLog entry = new SyncTaskLog();
            entry.setTriggerType(triggerType);
            entry.setStatus(status);
            entry.setRepoCount(repoCount);
            entry.setHiddenGone(hiddenGone);
            entry.setMessage(message);
            entry.setStartedAt(startedAt);
            entry.setFinishedAt(LocalDateTime.now());
            syncTaskLogMapper.insert(entry);
        } catch (Exception e) {
            log.warn("同步日志写入失败（不影响业务）: {}", e.getMessage());
        }
    }

    private String truncate(String message) {
        return message.length() <= LOG_MESSAGE_MAX ? message : message.substring(0, LOG_MESSAGE_MAX);
    }
}
