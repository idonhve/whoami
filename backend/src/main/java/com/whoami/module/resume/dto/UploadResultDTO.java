package com.whoami.module.resume.dto;

import java.util.List;

/** POST /admin/api/resumes 响应。evictedVersionNos 为本次上传自动淘汰的历史版本号（保留最近 3 个）。 */
public record UploadResultDTO(long id, int versionNo, List<Integer> evictedVersionNos) {
}
