package com.whoami.module.resume.dto;

import java.nio.file.Path;

/** 下载目标：物理文件路径 + 下载显示名（供 Content-Disposition 使用）。 */
public record ResumeDownload(Path path, String displayName) {
}
