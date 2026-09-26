package com.whoami.config;

import java.nio.file.Paths;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 静态资源映射：/uploads/** 只读暴露上传卷（Spec 07/08 的简历与证书图片）。
 * 静态资源处理器只响应 GET/HEAD，不提供写入口。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** 产物文件名含 UUID，内容不变，可长缓存 */
    private static final int CACHE_SECONDS = 7 * 24 * 60 * 60;

    private final AppProperties appProperties;

    public WebConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(toLocation(appProperties.uploadDir()))
                .setCachePeriod(CACHE_SECONDS);
    }

    /** Spring 要求 location 以 / 结尾才会拼接子路径 */
    private static String toLocation(String uploadDir) {
        String location = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }
}