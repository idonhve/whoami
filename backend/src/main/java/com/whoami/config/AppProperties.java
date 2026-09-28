package com.whoami.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors) {

    /** 跨域白名单：前端与后端分域部署（免费静态托管）时放行前端来源 */
    public record Cors(List<String> allowedOrigins) {
    }
}
