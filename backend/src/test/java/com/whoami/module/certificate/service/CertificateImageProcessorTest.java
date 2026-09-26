package com.whoami.module.certificate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.whoami.common.BizException;
import com.whoami.config.AppProperties;
import com.whoami.module.certificate.service.CertificateImageProcessor.ProcessedImage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片处理验收（Spec 08）：真实图片字节断言缩略图/压缩原图产物、魔数校验、体积上限。
 * 不依赖 Spring 容器与数据库。
 */
class CertificateImageProcessorTest {

    private static final int SOURCE_WIDTH = 2400;
    private static final int SOURCE_HEIGHT = 1600;

    @TempDir
    Path uploadDir;

    private CertificateImageProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new CertificateImageProcessor(new AppProperties(uploadDir.toString()));
    }

    @Test
    void jpegUploadProducesSmallerWebpThumbnailAndDownscaledOriginal() throws IOException {
        byte[] source = imageBytes(SOURCE_WIDTH, SOURCE_HEIGHT, "jpeg");

        ProcessedImage processed = processor.process(upload("cert.jpg", "image/jpeg", source));

        Path thumbnail = uploadDir.resolve(processed.thumbnailFile());
        Path original = uploadDir.resolve(processed.originalFile());
        assertThat(thumbnail).exists();
        assertThat(original).exists();
        // 产物为 WebP（RIFF....WEBP 魔数），且落在 uploads 卷 certificate/ 目录
        assertThat(processed.thumbnailFile()).startsWith("certificate/").endsWith(".webp");
        assertThat(processed.originalFile()).startsWith("certificate/").endsWith(".webp");
        assertThat(isWebp(Files.readAllBytes(thumbnail))).isTrue();
        assertThat(isWebp(Files.readAllBytes(original))).isTrue();

        // 缩略图约 400px 宽；压缩原图长边压到 ≤ 2000px
        BufferedImage thumbImage = ImageIO.read(thumbnail.toFile());
        BufferedImage originalImage = ImageIO.read(original.toFile());
        assertThat(thumbImage.getWidth()).isEqualTo(CertificateImageProcessor.THUMB_WIDTH);
        assertThat(thumbImage.getHeight())
                .isEqualTo(Math.round(SOURCE_HEIGHT * ((double) CertificateImageProcessor.THUMB_WIDTH / SOURCE_WIDTH)));
        assertThat(Math.max(originalImage.getWidth(), originalImage.getHeight()))
                .isEqualTo(CertificateImageProcessor.MAX_LONG_EDGE);

        // 缩略图体积显著小于压缩原图
        long thumbBytes = Files.size(thumbnail);
        long originalBytes = Files.size(original);
        assertThat(thumbBytes * 3).isLessThan(originalBytes);
    }

    @Test
    void pngAndWebpSourcesAreAccepted() throws IOException {
        byte[] png = imageBytes(1200, 800, "png");
        byte[] webp = imageBytes(1200, 800, "webp");

        ProcessedImage fromPng = processor.process(upload("scan.png", "image/png", png));
        ProcessedImage fromWebp = processor.process(upload("scan.webp", "image/webp", webp));

        assertThat(uploadDir.resolve(fromPng.thumbnailFile())).exists();
        assertThat(uploadDir.resolve(fromWebp.thumbnailFile())).exists();
    }

    @Test
    void smallerThanThumbnailSourceIsNotUpscaled() throws IOException {
        byte[] source = imageBytes(200, 150, "jpeg");

        ProcessedImage processed = processor.process(upload("tiny.jpg", "image/jpeg", source));

        BufferedImage thumbnail = ImageIO.read(uploadDir.resolve(processed.thumbnailFile()).toFile());
        assertThat(thumbnail.getWidth()).isEqualTo(200);
        assertThat(thumbnail.getHeight()).isEqualTo(150);
    }

    @Test
    void extensionDisguisedNonImageRejectedWith400() {
        // 文本/PDF 等改扩展名冒充 jpg：魔数不匹配
        MultipartFile disguised = upload("fake.jpg", "image/jpeg", "not an image at all".getBytes());

        assertThatThrownBy(() -> processor.process(disguised))
                .isInstanceOf(BizException.class)
                .satisfies(e -> {
                    assertThat(((BizException) e).getStatus()).isEqualTo(400);
                    assertThat(e.getMessage()).contains("格式不支持");
                });
    }

    @Test
    void corruptImageWithValidMagicRejectedWith400() {
        byte[] corrupt = new byte[512];
        corrupt[0] = (byte) 0xFF;
        corrupt[1] = (byte) 0xD8;
        corrupt[2] = (byte) 0xFF;

        assertThatThrownBy(() -> processor.process(upload("broken.jpg", "image/jpeg", corrupt)))
                .isInstanceOf(BizException.class)
                .satisfies(e -> {
                    assertThat(((BizException) e).getStatus()).isEqualTo(400);
                    assertThat(e.getMessage()).contains("无法解析");
                });
    }

    @Test
    void fileLargerThan5MbRejectedWith400() {
        byte[] oversize = new byte[(int) CertificateImageProcessor.MAX_FILE_BYTES + 1];
        oversize[0] = (byte) 0xFF;
        oversize[1] = (byte) 0xD8;
        oversize[2] = (byte) 0xFF;

        assertThatThrownBy(() -> processor.process(upload("huge.jpg", "image/jpeg", oversize)))
                .isInstanceOf(BizException.class)
                .satisfies(e -> {
                    assertThat(((BizException) e).getStatus()).isEqualTo(400);
                    assertThat(e.getMessage()).contains("5MB");
                });
    }

    @Test
    void missingOrEmptyFileRejectedWith400() {
        assertThatThrownBy(() -> processor.process(null))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getStatus()).isEqualTo(400));
        assertThatThrownBy(() -> processor.process(upload("empty.jpg", "image/jpeg", new byte[0])))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getStatus()).isEqualTo(400));
    }

    @Test
    void deleteRemovesBothProductsAndIgnoresMissingFiles() throws IOException {
        ProcessedImage processed = processor.process(
                upload("cert.jpg", "image/jpeg", imageBytes(800, 600, "jpeg")));
        Path thumbnail = uploadDir.resolve(processed.thumbnailFile());
        Path original = uploadDir.resolve(processed.originalFile());

        processor.delete(processed.originalFile(), processed.thumbnailFile());
        assertThat(thumbnail).doesNotExist();
        assertThat(original).doesNotExist();

        // 重复删除 / 空值不抛异常
        processor.delete(processed.originalFile(), null, "  ");
    }

    private static MultipartFile upload(String filename, String contentType, byte[] bytes) {
        return new MockMultipartFile("file", filename, contentType, bytes);
    }

    /** 生成带噪点的真实图片字节（噪点避免纯色导致压缩体积失真） */
    private static byte[] imageBytes(int width, int height, String format) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int[] pixels = new int[width * height];
        Random random = new Random(20260926L);
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = random.nextInt(0xFFFFFF) | 0xFF000000;
        }
        image.setRGB(0, 0, width, height, pixels, 0, width);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    private static boolean isWebp(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }
}