package com.whoami.module.certificate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.whoami.common.BizException;
import com.whoami.module.certificate.dto.CertificateDTO;
import com.whoami.module.certificate.dto.UpdateCertificateRequest;
import com.whoami.module.certificate.entity.Certificate;
import com.whoami.module.certificate.mapper.CertificateMapper;
import com.whoami.module.certificate.service.CertificateImageProcessor.ProcessedImage;
import com.whoami.module.upload.service.UploadBlobService;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 证书 CRUD（Spec 08）：上传即生成缩略图 + 压缩原图，删除时内容行一并清理。
 * 图片产物存 upload_blob（容器平台磁盘易失），经 /uploads/** 只读接口对外暴露。
 */
@Service
public class CertificateService {

    /** name 长度上限（Spec 08 表结构 varchar(100)） */
    public static final int MAX_NAME_LENGTH = 100;

    private static final String UPLOAD_URL_PREFIX = "/uploads/";

    private final CertificateMapper certificateMapper;
    private final CertificateImageProcessor imageProcessor;
    private final UploadBlobService uploadBlobService;

    public CertificateService(CertificateMapper certificateMapper, CertificateImageProcessor imageProcessor,
                              UploadBlobService uploadBlobService) {
        this.certificateMapper = certificateMapper;
        this.imageProcessor = imageProcessor;
        this.uploadBlobService = uploadBlobService;
    }

    /** 前台列表：sortOrder 升序，其次 obtainedAt 倒序 */
    public List<CertificateDTO> listPublic() {
        return certificateMapper.selectList(new LambdaQueryWrapper<Certificate>()
                        .orderByAsc(Certificate::getSortOrder)
                        .orderByDesc(Certificate::getObtainedAt))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public long create(MultipartFile file, String name, String obtainedAt) {
        String validName = requireName(name);
        LocalDate validObtainedAt = requireObtainedAt(obtainedAt);
        ProcessedImage processed = imageProcessor.process(file);

        Certificate entity = new Certificate();
        entity.setName(validName);
        entity.setObtainedAt(validObtainedAt);
        entity.setOriginalFile(processed.originalFile());
        entity.setThumbnailFile(processed.thumbnailFile());
        try {
            uploadBlobService.store(processed.originalFile(), processed.contentType(), processed.original());
            if (!processed.thumbnailFile().equals(processed.originalFile())) {
                uploadBlobService.store(processed.thumbnailFile(), processed.contentType(), processed.thumbnail());
            }
            certificateMapper.insert(entity);
        } catch (RuntimeException e) {
            // 入库失败则清理刚存的内容行，避免孤儿文件
            uploadBlobService.delete(processed.originalFile());
            if (!processed.thumbnailFile().equals(processed.originalFile())) {
                uploadBlobService.delete(processed.thumbnailFile());
            }
            throw e;
        }
        return entity.getId();
    }

    public void update(long id, UpdateCertificateRequest request) {
        if (certificateMapper.selectById(id) == null) {
            throw new BizException(404, "证书不存在: " + id);
        }
        Certificate update = new Certificate();
        update.setId(id);
        if (request.name() != null) {
            update.setName(requireName(request.name()));
        }
        if (request.obtainedAt() != null) {
            update.setObtainedAt(request.obtainedAt());
        }
        if (request.sortOrder() != null) {
            update.setSortOrder(request.sortOrder());
        }
        certificateMapper.updateById(update);
    }

    public void delete(long id) {
        Certificate existing = certificateMapper.selectById(id);
        if (existing == null) {
            throw new BizException(404, "证书不存在: " + id);
        }
        certificateMapper.deleteById(id);
        uploadBlobService.delete(existing.getOriginalFile());
        if (!existing.getThumbnailFile().equals(existing.getOriginalFile())) {
            uploadBlobService.delete(existing.getThumbnailFile());
        }
    }

    private CertificateDTO toDTO(Certificate entity) {
        return new CertificateDTO(
                entity.getId(),
                entity.getName(),
                entity.getObtainedAt(),
                UPLOAD_URL_PREFIX + entity.getThumbnailFile(),
                UPLOAD_URL_PREFIX + entity.getOriginalFile(),
                entity.getSortOrder() == null ? 0 : entity.getSortOrder());
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new BizException(400, "name 必填");
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new BizException(400, "name 长度不能超过 " + MAX_NAME_LENGTH);
        }
        return trimmed;
    }

    private LocalDate requireObtainedAt(String obtainedAt) {
        if (obtainedAt == null || obtainedAt.isBlank()) {
            throw new BizException(400, "obtainedAt 必填");
        }
        try {
            return LocalDate.parse(obtainedAt.trim());
        } catch (DateTimeParseException e) {
            throw new BizException(400, "obtainedAt 格式错误，应为 yyyy-MM-dd");
        }
    }
}
