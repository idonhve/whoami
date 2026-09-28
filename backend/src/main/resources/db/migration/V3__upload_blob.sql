-- V3__upload_blob.sql — 上传文件持久化改存数据库（免费容器平台磁盘易失）
-- 背景：Render 免费层等容器平台每次休眠/重启都会丢弃本地磁盘，
-- 简历 PDF 与证书图片若落盘会丢失。统一改存 upload_blob 表，/uploads/** 由接口流式返回。
-- LONGBLOB 上限 4GB，覆盖 20MB 简历上限；文件名 UUID 不可猜。

CREATE TABLE upload_blob
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    file_path    VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    content      LONGBLOB     NOT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_upload_blob_file_path (file_path)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '上传文件内容（resume/certificate 模块共用，URL 契约 /uploads/** 不变）';
