package com.whoami.module.certificate.service;

import com.whoami.common.BizException;
import com.whoami.config.AppProperties;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 证书图片处理（Spec 08）：上传原图 → 缩略图（约 400px 宽，前台网格）+ 压缩原图（长边 ≤ 2000px，灯箱）。
 * 产物一律 WebP（体积最优）；运行环境无 WebP 编码器时回退 JPEG。
 * 存储于 uploads 卷 certificate/ 目录，文件名用 UUID，原图文件名不落库、不外露。
 */
@Component
public class CertificateImageProcessor {

    private static final Logger log = LoggerFactory.getLogger(CertificateImageProcessor.class);

    /** 缩略图宽度（前台网格用） */
    public static final int THUMB_WIDTH = 400;
    /** 压缩原图长边上限（灯箱用） */
    public static final int MAX_LONG_EDGE = 2000;
    /** 单张上传上限（Spec 08） */
    public static final long MAX_FILE_BYTES = 5L * 1024 * 1024;

    private static final String DIR_NAME = "certificate";
    private static final String WEBP = "webp";
    private static final String JPEG = "jpeg";
    private static final float THUMB_QUALITY = 0.8f;
    private static final float IMAGE_QUALITY = 0.85f;

    private final Path uploadRoot;
    private final Path certificateDir;

    public CertificateImageProcessor(AppProperties appProperties) {
        this.uploadRoot = Paths.get(appProperties.uploadDir()).toAbsolutePath().normalize();
        this.certificateDir = uploadRoot.resolve(DIR_NAME);
    }

    /** 落盘产物：相对 app.upload-dir 的路径（正斜杠，可直接拼 /uploads/ 供前台访问） */
    public record ProcessedImage(String originalFile, String thumbnailFile) {
    }

    /**
     * 校验并处理上传图片。校验失败抛 400；处理/落盘失败抛 500。
     */
    public ProcessedImage process(MultipartFile file) {
        byte[] bytes = readBytes(file);
        if (ImageFormat.detect(bytes) == null) {
            throw new BizException(400, "图片格式不支持，仅支持 jpg/jpeg/png/webp");
        }
        BufferedImage source = decode(bytes);

        String format = ImageIO.getImageWritersByFormatName(WEBP).hasNext() ? WEBP : JPEG;
        String extension = WEBP.equals(format) ? "webp" : "jpg";
        String base = UUID.randomUUID().toString().replace("-", "");

        try {
            Files.createDirectories(certificateDir);
            Path thumbnail = certificateDir.resolve(base + "_thumb." + extension);
            Path original = certificateDir.resolve(base + "." + extension);

            int[] thumbSize = fit(source.getWidth(), source.getHeight(), THUMB_WIDTH, Integer.MAX_VALUE);
            encode(source, thumbSize, thumbnail, format, THUMB_QUALITY);

            int[] imageSize = fit(source.getWidth(), source.getHeight(), MAX_LONG_EDGE, MAX_LONG_EDGE);
            encode(source, imageSize, original, format, IMAGE_QUALITY);

            return new ProcessedImage(DIR_NAME + "/" + original.getFileName(), DIR_NAME + "/" + thumbnail.getFileName());
        } catch (IOException e) {
            log.error("证书图片处理失败", e);
            throw new BizException(500, "图片处理失败");
        }
    }

    /** 删除落盘产物（记录删除后调用；失败只告警，不回滚已删记录） */
    public void delete(String... relativePaths) {
        for (String relativePath : relativePaths) {
            if (relativePath == null || relativePath.isBlank()) {
                continue;
            }
            Path target = uploadRoot.resolve(relativePath).normalize();
            if (!target.startsWith(uploadRoot)) {
                log.warn("跳过非法路径的证书文件删除: {}", relativePath);
                continue;
            }
            try {
                Files.deleteIfExists(target);
            } catch (IOException e) {
                log.warn("证书文件删除失败: {} ({})", relativePath, e.getMessage());
            }
        }
    }

    private byte[] readBytes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "file 必填");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BizException(400, "图片大小不能超过 5MB");
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            log.error("读取上传文件失败", e);
            throw new BizException(500, "文件读取失败");
        }
    }

    private BufferedImage decode(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new BizException(400, "图片内容无法解析");
            }
            return image;
        } catch (IOException e) {
            throw new BizException(400, "图片内容无法解析");
        }
    }

    /** 按比例缩放到目标框内，不放大原图 */
    private static int[] fit(int width, int height, int maxWidth, int maxHeight) {
        double scale = Math.min(1.0, Math.min((double) maxWidth / width, (double) maxHeight / height));
        return new int[] {(int) Math.round(width * scale), (int) Math.round(height * scale)};
    }

    private void encode(BufferedImage source, int[] size, Path dest, String format, float quality) throws IOException {
        BufferedImage target = JPEG.equals(format) ? flattenAlpha(source) : source;
        Thumbnails.of(target)
                .size(size[0], size[1])
                .outputFormat(format)
                .outputQuality(quality)
                .toFile(dest.toFile());
    }

    /** JPEG 无 alpha 通道：透明区域先铺白，避免 PNG 转 JPEG 后出现黑块 */
    private static BufferedImage flattenAlpha(BufferedImage source) {
        if (!source.getColorModel().hasAlpha()) {
            return source;
        }
        BufferedImage flattened = new BufferedImage(
                source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = flattened.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return flattened;
    }

    /** 上传文件魔数识别（不信扩展名） */
    private enum ImageFormat {
        JPEG, PNG, WEBP;

        static ImageFormat detect(byte[] bytes) {
            if (bytes.length >= 3
                    && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
                return JPEG;
            }
            if (bytes.length >= 8
                    && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                    && (bytes[4] & 0xFF) == 0x0D && (bytes[5] & 0xFF) == 0x0A
                    && (bytes[6] & 0xFF) == 0x1A && (bytes[7] & 0xFF) == 0x0A) {
                return PNG;
            }
            if (bytes.length >= 12
                    && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
                return WEBP;
            }
            return null;
        }
    }
}