package com.whoami.module.track.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 访问日志（Spec 05）：一次会话一行，session_id 唯一（前端 crypto.randomUUID() 生成）。
 * IP 只存库用于归属地解析与追溯，任何接口不得外发原文（IP 脱敏红线）。
 */
@TableName("visit_log")
public class VisitLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;

    /** 支持 IPv6 长度；展示层必须脱敏 */
    private String ip;

    /** ip2region 解析（境外 / 解析失败为 null） */
    private String province;

    /** ip2region 解析（境外 / 解析失败为 null） */
    private String city;

    /** 原始 UA（截断到列宽 500） */
    private String userAgent;

    /** 来源页 */
    private String referrer;

    /** 进入页路径 */
    private String entryPage;

    private LocalDateTime entryTime;

    /** end 上报写入 */
    private LocalDateTime leaveTime;

    /** 服务端计算（leave_time - entry_time 秒数） */
    private Integer durationSeconds;

    /** 聚合用（entry_time 的日期） */
    private LocalDate visitDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    public String getEntryPage() {
        return entryPage;
    }

    public void setEntryPage(String entryPage) {
        this.entryPage = entryPage;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(LocalDateTime entryTime) {
        this.entryTime = entryTime;
    }

    public LocalDateTime getLeaveTime() {
        return leaveTime;
    }

    public void setLeaveTime(LocalDateTime leaveTime) {
        this.leaveTime = leaveTime;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(LocalDate visitDate) {
        this.visitDate = visitDate;
    }
}
