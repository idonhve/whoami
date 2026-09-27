package com.whoami.module.track.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 行为事件（Spec 05）：六类事件（见 EventType）。
 * resume_download 亦由 ResumeService 在简历下载时服务端直写（Spec 07），两处口径必须一致。
 */
@TableName("track_event")
public class TrackEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** EventType 枚举值字符串 */
    private String eventType;

    /** 关联 visit_log.session_id（不做外键约束，事件允许先于/独立于会话） */
    private String sessionId;

    private String pagePath;

    /** 事件明细 JSON 字符串（如命令面板命令名），以合法 JSON 文本存储 */
    private String detail;

    private String ip;

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getPagePath() {
        return pagePath;
    }

    public void setPagePath(String pagePath) {
        this.pagePath = pagePath;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
