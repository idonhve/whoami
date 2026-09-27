package com.whoami.module.guestmessage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 访客留言（Spec 05）：访客自愿留名，默认 approved 自动通过。
 * email 仅站主可见（公开 DTO 绝不含）；content 纯文本存储；ip 仅用于限流与追溯，对外必须脱敏。
 */
@TableName("guest_message")
public class GuestMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** ≤ 20 字符 */
    private String nickname;

    /** 可空；仅后台可见 */
    private String email;

    /** 纯文本 ≤ 500 */
    private String content;

    /** approved / hidden */
    private String status;

    /** 管理员回复 */
    private String reply;

    private LocalDateTime repliedAt;

    /** 限流与追溯用（同 IP 每分钟 ≤ 3 条） */
    private String ip;

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public LocalDateTime getRepliedAt() {
        return repliedAt;
    }

    public void setRepliedAt(LocalDateTime repliedAt) {
        this.repliedAt = repliedAt;
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
