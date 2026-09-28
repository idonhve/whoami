package com.whoami.module.upload.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.whoami.module.upload.entity.UploadBlob;
import com.whoami.module.upload.mapper.UploadBlobMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 上传文件存取（resume/certificate 共用）：内容落 upload_blob 表，替代易失磁盘。
 * 业务表只存相对路径（filePath），文件体在此集中读写；URL 契约 /uploads/** 不变。
 */
@Service
public class UploadBlobService {

    private static final Logger log = LoggerFactory.getLogger(UploadBlobService.class);

    /** 读取结果：内容类型 + 字节体 */
    public record StoredBlob(String contentType, byte[] content) {
    }

    private final UploadBlobMapper uploadBlobMapper;

    public UploadBlobService(UploadBlobMapper uploadBlobMapper) {
        this.uploadBlobMapper = uploadBlobMapper;
    }

    public void store(String filePath, String contentType, byte[] content) {
        UploadBlob blob = new UploadBlob();
        blob.setFilePath(filePath);
        blob.setContentType(contentType);
        blob.setContent(content);
        uploadBlobMapper.insert(blob);
    }

    /** 按相对路径读取；不存在返回 null（由调用方决定 404 语义） */
    public StoredBlob load(String filePath) {
        UploadBlob blob = uploadBlobMapper.selectOne(new LambdaQueryWrapper<UploadBlob>()
                .eq(UploadBlob::getFilePath, filePath)
                .last("LIMIT 1"));
        return blob == null ? null : new StoredBlob(blob.getContentType(), blob.getContent());
    }

    /** 删除内容行；失败仅告警（记录已删的场景不回滚） */
    public void delete(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        try {
            uploadBlobMapper.delete(new LambdaQueryWrapper<UploadBlob>()
                    .eq(UploadBlob::getFilePath, filePath));
        } catch (RuntimeException e) {
            log.warn("上传内容删除失败（filePath={}）：{}", filePath, e.getMessage());
        }
    }
}
