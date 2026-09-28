package com.whoami.module.upload.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 上传文件内容（resume/certificate 共用）。免费容器平台磁盘易失，文件体改存 DB。 */
@TableName("upload_blob")
public class UploadBlob {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 存储相对路径，如 resume/<uuid>-v3.pdf、certificate/<uuid>_thumb.webp */
    private String filePath;

    private String contentType;

    private byte[] content;

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
