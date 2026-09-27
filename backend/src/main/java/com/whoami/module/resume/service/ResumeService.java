package com.whoami.module.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whoami.common.BizException;
import com.whoami.config.AppProperties;
import com.whoami.config.UploadConfig;
import com.whoami.module.resume.dto.ResumeDownload;
import com.whoami.module.resume.dto.ResumeLatestDTO;
import com.whoami.module.resume.dto.ResumeVersionDTO;
import com.whoami.module.resume.dto.UploadResultDTO;
import com.whoami.module.resume.entity.ResumeFile;
import com.whoami.module.resume.mapper.ResumeFileMapper;
import com.whoami.module.siteconfig.dto.SiteConfigDTO;
import com.whoami.module.siteconfig.service.SiteConfigService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** 简历版本（Spec 07）：上传/版本列表/回滚/下载 + 下载埋点（服务端直写 track_event）。 */
@Service
public class ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);

    /** 校验只看魔数，不信扩展名 */
    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F', '-'};

    /** 保留最近 N 个版本（PRD 明确数量），超出即删旧 */
    private static final int RETAIN_VERSIONS = 3;

    private static final String RESUME_DIR = "resume";
    private static final String EVENT_RESUME_DOWNLOAD = "resume_download";

    private final ResumeFileMapper resumeFileMapper;
    private final SiteConfigService siteConfigService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final String uploadRoot;

    public ResumeService(ResumeFileMapper resumeFileMapper, SiteConfigService siteConfigService,
                         JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, AppProperties appProperties) {
        this.resumeFileMapper = resumeFileMapper;
        this.siteConfigService = siteConfigService;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.uploadRoot = appProperties.uploadDir();
    }

    /** 公开 latest：按钮显隐与文案 */
    public ResumeLatestDTO latest() {
        ResumeFile current = current();
        if (current == null) {
            return new ResumeLatestDTO(false, null, null);
        }
        return new ResumeLatestDTO(true, current.getDisplayName(), current.getUploadedAt());
    }

    /**
     * 下载：先直写 resume_download 埋点（浏览器导航下载无法保证前端上报），再返回当前版文件。
     * 无版本或物理文件缺失时 404。
     */
    public ResumeDownload download(HttpServletRequest request) {
        ResumeFile current = current();
        if (current == null) {
            throw new BizException(404, "尚未上传简历");
        }
        Path physical = uploadRootPath(current.getFilePath());
        if (!Files.exists(physical)) {
            throw new BizException(404, "简历文件已丢失");
        }
        recordDownloadEvent(current, request);
        return new ResumeDownload(physical, current.getDisplayName());
    }

    /**
     * 上传：递增版本号 + 置为新当前版 + 淘汰超出保留数量的最旧版（记录与物理文件一并删除）。
     * 事务保证 is_current 全表至多一个 1。
     */
    @Transactional
    public UploadResultDTO upload(MultipartFile file) {
        validateFile(file);
        int versionNo = nextVersionNo();
        LocalDateTime uploadedAt = LocalDateTime.now();
        String filePath = storeFile(file, versionNo);
        ResumeFile entity = new ResumeFile();
        entity.setVersionNo(versionNo);
        entity.setFilePath(filePath);
        entity.setDisplayName(buildDisplayName(uploadedAt));
        entity.setSizeBytes(file.getSize());
        entity.setIsCurrent(true);
        entity.setUploadedAt(uploadedAt);
        try {
            resumeFileMapper.update(null, new LambdaUpdateWrapper<ResumeFile>()
                    .set(ResumeFile::getIsCurrent, false)
                    .eq(ResumeFile::getIsCurrent, true));
            resumeFileMapper.insert(entity);
        } catch (RuntimeException e) {
            deleteFileQuietly(uploadRootPath(filePath));
            throw e;
        }
        List<Integer> evicted = evictOldest();
        return new UploadResultDTO(entity.getId(), versionNo, evicted);
    }

    /** 后台版本列表，versionNo 倒序 */
    public List<ResumeVersionDTO> listAdmin() {
        return resumeFileMapper.selectList(new LambdaQueryWrapper<ResumeFile>()
                        .orderByDesc(ResumeFile::getVersionNo))
                .stream()
                .map(this::toVersionDTO)
                .toList();
    }

    /** 回滚：把指定历史版本置为当前版，不删除其它版本 */
    @Transactional
    public void restore(long id) {
        if (resumeFileMapper.selectById(id) == null) {
            throw new BizException(404, "简历版本不存在: " + id);
        }
        resumeFileMapper.update(null, new LambdaUpdateWrapper<ResumeFile>()
                .set(ResumeFile::getIsCurrent, false)
                .eq(ResumeFile::getIsCurrent, true));
        ResumeFile update = new ResumeFile();
        update.setId(id);
        update.setIsCurrent(true);
        resumeFileMapper.updateById(update);
    }

    private ResumeFile current() {
        return resumeFileMapper.selectOne(new LambdaQueryWrapper<ResumeFile>()
                .eq(ResumeFile::getIsCurrent, true)
                .last("LIMIT 1"));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "缺少文件");
        }
        if (file.getSize() > UploadConfig.MAX_FILE_BYTES) {
            throw new BizException(400, "文件超过 20MB 上限");
        }
        try (InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(PDF_MAGIC.length);
            if (!Arrays.equals(head, PDF_MAGIC)) {
                throw new BizException(400, "仅支持 PDF 文件（需以 %PDF- 开头）");
            }
        } catch (IOException e) {
            throw new BizException(400, "文件读取失败");
        }
    }

    private int nextVersionNo() {
        ResumeFile max = resumeFileMapper.selectOne(new LambdaQueryWrapper<ResumeFile>()
                .orderByDesc(ResumeFile::getVersionNo)
                .last("LIMIT 1"));
        return max == null ? 1 : max.getVersionNo() + 1;
    }

    private String storeFile(MultipartFile file, int versionNo) {
        try {
            Path resumeDir = uploadRootPath(RESUME_DIR);
            Files.createDirectories(resumeDir);
            String storageName = UUID.randomUUID().toString().replace("-", "") + "-v" + versionNo + ".pdf";
            Path target = resumeDir.resolve(storageName);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return RESUME_DIR + "/" + storageName;
        } catch (IOException e) {
            throw new BizException(400, "文件保存失败");
        }
    }

    private String buildDisplayName(LocalDateTime uploadedAt) {
        SiteConfigDTO owner = siteConfigService.findByKey("owner_name");
        String ownerName = owner == null || owner.value() == null ? "" : owner.value().trim();
        String yearMonth = uploadedAt.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return (ownerName.isBlank() ? "" : ownerName + "_") + "简历_" + yearMonth + ".pdf";
    }

    /**
     * 淘汰超出保留数量的最旧版（记录与物理文件一并删除）。
     * 保留规则：当前版 + 最新 (RETAIN_VERSIONS-1) 个历史版——回滚到旧版后继续上传时，
     * 旧当前版不能被"按版本号最旧"淘汰，否则 latest 会失效。
     */
    private List<Integer> evictOldest() {
        List<ResumeFile> all = resumeFileMapper.selectList(new LambdaQueryWrapper<ResumeFile>()
                .orderByDesc(ResumeFile::getVersionNo));
        if (all.size() <= RETAIN_VERSIONS) {
            return List.of();
        }
        List<ResumeFile> keep = new ArrayList<>();
        ResumeFile current = null;
        for (ResumeFile file : all) {
            if (Boolean.TRUE.equals(file.getIsCurrent())) {
                current = file;
            } else if (keep.size() < RETAIN_VERSIONS - 1) {
                keep.add(file);
            }
        }
        List<Integer> evicted = new ArrayList<>();
        for (ResumeFile file : all) {
            if (file == current || keep.contains(file)) {
                continue;
            }
            resumeFileMapper.deleteById(file.getId());
            deleteFileQuietly(uploadRootPath(file.getFilePath()));
            evicted.add(file.getVersionNo());
        }
        return evicted;
    }

    private void recordDownloadEvent(ResumeFile file, HttpServletRequest request) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO track_event (event_type, session_id, page_path, detail, ip) VALUES (?, ?, ?, ?, ?)",
                    EVENT_RESUME_DOWNLOAD,
                    UUID.randomUUID().toString(),
                    "/api/resume/download",
                    objectMapper.writeValueAsString(java.util.Map.of("versionNo", file.getVersionNo(),
                            "displayName", file.getDisplayName())),
                    resolveIp(request));
        } catch (Exception e) {
            log.warn("下载埋点写入失败（不影响下载）: {}", e.getMessage());
        }
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private long entityIdOf(int versionNo, String filePath) {
        ResumeFile inserted = resumeFileMapper.selectOne(new LambdaQueryWrapper<ResumeFile>()
                .eq(ResumeFile::getVersionNo, versionNo)
                .eq(ResumeFile::getFilePath, filePath)
                .last("LIMIT 1"));
        return inserted == null ? 0 : inserted.getId();
    }

    private Path uploadRootPath(String relative) {
        return Path.of(uploadRoot).resolve(relative).normalize();
    }

    private void deleteFileQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("物理文件删除失败（记录已删）: {}", path);
        }
    }

    private ResumeVersionDTO toVersionDTO(ResumeFile entity) {
        return new ResumeVersionDTO(
                entity.getId(),
                entity.getVersionNo(),
                entity.getDisplayName(),
                entity.getSizeBytes() == null ? 0 : entity.getSizeBytes(),
                Boolean.TRUE.equals(entity.getIsCurrent()),
                entity.getUploadedAt());
    }
}
