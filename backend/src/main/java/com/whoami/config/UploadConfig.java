package com.whoami.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

/**
 * 上传体积上限（Spec 07/08 均要求 ≤ 20MB）。
 * multipart 解析器上限设为 21MB，仅作粗防线：20MB 的业务上限在 Controller 内校验并返回
 * 干净的 400（若解析器与业务上限相同，超限文件会在客户端传完前被掐断连接，响应无法送达）。
 * 用 Bean 而非 application.yml 配置，避免改动全局配置文件。
 */
@Configuration
public class UploadConfig {

    public static final long MAX_FILE_BYTES = 20L * 1024 * 1024;

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofBytes(21L * 1024 * 1024));
        // 请求体积略大于单文件上限，容纳 multipart 分界与表单开销
        factory.setMaxRequestSize(DataSize.ofBytes(23L * 1024 * 1024));
        return factory.createMultipartConfig();
    }
}
