package com.whoami.module.github.dto;

/** 同步结果（POST /admin/api/projects/sync 响应；失败时 200 包络内 status=failed + 原因） */
public record SyncResult(String status, int repoCount, int hiddenGone, String message) {

    public static SyncResult success(int repoCount, int hiddenGone) {
        return new SyncResult("success", repoCount, hiddenGone, null);
    }

    public static SyncResult failed(String message) {
        return new SyncResult("failed", 0, 0, message);
    }
}
