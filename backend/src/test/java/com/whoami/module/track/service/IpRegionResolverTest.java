package com.whoami.module.track.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.whoami.module.track.service.IpRegionResolver.IpRegion;
import org.junit.jupiter.api.Test;

/**
 * ip2region 离线库解析断言（Spec 05 验收：内置几组已知 IP）。
 * 选用长期归属稳定的公共 IP：114DNS（江苏南京）、百度（北京）、Google DNS（境外）。
 * 境外 / 保留 / 非法 IP 一律降级为空（province/city 为 null，符合"境外可为空"口径）。
 */
class IpRegionResolverTest {

    private final IpRegionResolver resolver = new IpRegionResolver();

    @Test
    void resolvesNanjingDnsToJiangsuNanjing() {
        IpRegion region = resolver.resolve("114.114.114.114");
        assertThat(region.province()).contains("江苏");
        assertThat(region.city()).contains("南京");
    }

    @Test
    void resolvesBaiduIpToBeijing() {
        IpRegion region = resolver.resolve("220.181.38.148");
        assertThat(region.province()).contains("北京");
    }

    @Test
    void foreignIpHasNoProvince() {
        assertThat(resolver.resolve("8.8.8.8").province()).isNull();
        assertThat(resolver.resolve("8.8.8.8").city()).isNull();
    }

    @Test
    void loopbackReservedAndInvalidIpsFallBackToEmpty() {
        assertThat(resolver.resolve("127.0.0.1").province()).isNull();
        assertThat(resolver.resolve("192.168.1.100").province()).isNull();
        assertThat(resolver.resolve("not-an-ip").province()).isNull();
        assertThat(resolver.resolve(null).province()).isNull();
        assertThat(resolver.resolve("  ").province()).isNull();
    }

    @Test
    void whitespaceAroundIpIsTolerated() {
        IpRegion region = resolver.resolve(" 114.114.114.114 ");
        assertThat(region.province()).contains("江苏");
    }
}
