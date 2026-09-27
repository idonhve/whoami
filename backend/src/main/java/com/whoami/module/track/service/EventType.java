package com.whoami.module.track.service;

import com.whoami.common.BizException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 埋点事件类型（Spec 05，PRD §7.2 代决项为基线）。
 * resume_download 亦由 ResumeService 在简历下载时服务端直写 track_event（Spec 07），
 * 两处共用字符串口径 "resume_download"，不得改名。
 */
public enum EventType {
    PAGE_VIEW("page_view"),
    RESUME_DOWNLOAD("resume_download"),
    CMD_PALETTE_USE("cmd_palette_use"),
    EASTER_EGG("easter_egg"),
    GITHUB_OUTBOUND("github_outbound"),
    MESSAGE_SUBMIT("message_submit");

    private static final String ALLOWED_VALUES =
            Arrays.stream(values()).map(EventType::value).collect(Collectors.joining("/"));

    private final String value;

    EventType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    /** 非枚举值一律 400 */
    public static EventType parse(String value) {
        for (EventType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new BizException(400, "eventType 必须是 " + ALLOWED_VALUES + " 之一");
    }
}
