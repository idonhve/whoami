package com.whoami.module.techstack.service;

import com.whoami.common.BizException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Validates small raster icons by file signature and decoded image dimensions. */
@Component
public class TechIconProcessor {

    public static final long MAX_FILE_BYTES = 512L * 1024;
    private static final long MAX_PIXELS = 1_048_576L;

    public record ProcessedIcon(String filePath, String contentType, byte[] content) { }

    public ProcessedIcon process(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "请上传技术图标");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BizException(400, "图标不能超过 512 KB");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BizException(400, "读取图标失败");
        }
        String extension = detectExtension(bytes);
        if (extension == null) {
            throw new BizException(400, "图标格式不支持，请使用 PNG、JPG、JPEG 或 WebP");
        }
        validateDimensions(bytes);

        String contentType = switch (extension) {
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
        return new ProcessedIcon(
                "tech-icon/" + UUID.randomUUID().toString().replace("-", "") + "." + extension,
                contentType,
                bytes);
    }

    private String detectExtension(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                && (bytes[4] & 0xFF) == 0x0D && (bytes[5] & 0xFF) == 0x0A
                && (bytes[6] & 0xFF) == 0x1A && (bytes[7] & 0xFF) == 0x0A) {
            return "png";
        }
        if (bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "webp";
        }
        return null;
    }

    private void validateDimensions(byte[] bytes) {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (stream == null) {
                throw new BizException(400, "图标内容无法解析");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw new BizException(400, "图标内容无法解析");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 1024 || height > 1024
                        || (long) width * height > MAX_PIXELS) {
                    throw new BizException(400, "图标尺寸不能超过 1024 × 1024 像素");
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new BizException(400, "图标内容无法解析");
        }
    }
}
