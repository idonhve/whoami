package com.whoami.module.github;

/** GitHub 同步失败（PAT 缺失/无效、网络异常等），由 SyncService 转为 failed 日志，不向上抛 500 */
public class GitHubSyncException extends RuntimeException {

    public GitHubSyncException(String message) {
        super(message);
    }

    public GitHubSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
