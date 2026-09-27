package com.whoami.module.track.service;

import java.io.InputStream;
import org.lionsoul.ip2region.xdb.Searcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * IP 归属地离线解析（Spec 05 / ADR-0002）：classpath:ip2region.xdb 全量内存加载，零外部 API 依赖。
 * xdb 基于内容缓存（newWithBuffer）的 Searcher 无内部可变状态，可安全并发查询。
 * 解析失败 / 境外 / 保留地址一律降级为空 IpRegion（province/city 为 null，符合"境外可为空"口径），
 * 绝不影响埋点主流程。region 原文格式：国家|区域|省份|城市|ISP，省份/城市为 "0" 视为空。
 */
@Service
public class IpRegionResolver {

    private static final Logger log = LoggerFactory.getLogger(IpRegionResolver.class);

    /** 与 visit_log.province / city 列宽一致 */
    private static final int MAX_LENGTH = 50;

    private final Searcher searcher;

    public IpRegionResolver() {
        this.searcher = load();
    }

    private static Searcher load() {
        try (InputStream in = new ClassPathResource("ip2region.xdb").getInputStream()) {
            Searcher loaded = Searcher.newWithBuffer(in.readAllBytes());
            log.info("ip2region.xdb 已加载（离线 IP 归属地解析就绪）");
            return loaded;
        } catch (Exception e) {
            // 库文件缺失 / 损坏时降级为不解析，埋点照常入库（province/city 为空）
            log.warn("ip2region.xdb 加载失败，IP 归属地解析降级为空: {}", e.getMessage());
            return null;
        }
    }

    public IpRegion resolve(String ip) {
        if (searcher == null || ip == null || ip.isBlank()) {
            return IpRegion.EMPTY;
        }
        try {
            String region = searcher.search(ip.trim());
            String[] parts = region.split("\\|");
            String province = clean(parts.length > 2 ? parts[2] : null);
            String city = clean(parts.length > 3 ? parts[3] : null);
            return new IpRegion(province, city);
        } catch (Exception e) {
            return IpRegion.EMPTY;
        }
    }

    /** "0" / 空白视为未解析；超长截断到列宽 */
    private static String clean(String part) {
        if (part == null || part.isBlank() || "0".equals(part)) {
            return null;
        }
        return part.length() <= MAX_LENGTH ? part : part.substring(0, MAX_LENGTH);
    }

    /** 归属地：境外 / 未解析时 province 与 city 均为 null */
    public record IpRegion(String province, String city) {

        static final IpRegion EMPTY = new IpRegion(null, null);
    }
}
