package com.whoami.module.guestmessage.support;

/**
 * IP 展示脱敏（Spec 05 IP 红线）：原始 IP 只存库，任何接口响应不返回原文。
 * 后台确需展示时输出 1.2.*.* 格式：IPv4 保留前两段，IPv6 保留前两组，其余以 * 代替。
 */
public final class IpMasker {

    private IpMasker() {
    }

    public static String mask(String ip) {
        if (ip == null || ip.isBlank()) {
            return "";
        }
        String trimmed = ip.trim();
        String[] v4 = trimmed.split("\\.");
        if (v4.length == 4) {
            return v4[0] + "." + v4[1] + ".*.*";
        }
        String[] v6 = trimmed.split(":");
        if (v6.length >= 2) {
            return v6[0] + ":" + v6[1] + ":*";
        }
        return "*";
    }
}
