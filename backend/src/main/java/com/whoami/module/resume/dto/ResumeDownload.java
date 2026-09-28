package com.whoami.module.resume.dto;

/** 下载目标：文件字节（存 DB）+ 下载显示名（供 Content-Disposition 使用）。 */
public record ResumeDownload(byte[] content, String displayName) {
}
