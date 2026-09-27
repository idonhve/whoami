package com.whoami.module.guestmessage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.whoami.common.BizException;
import com.whoami.module.guestmessage.dto.AdminMessageDTO;
import com.whoami.module.guestmessage.dto.MessageCreateRequest;
import com.whoami.module.guestmessage.dto.MessageDTO;
import com.whoami.module.guestmessage.entity.GuestMessage;
import com.whoami.module.guestmessage.mapper.GuestMessageMapper;
import com.whoami.module.guestmessage.support.IpMasker;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 访客留言（Spec 05）：提交（同 IP 每分钟 ≤ 3 条限流，DB 按 ip + created_at 一分钟窗口计数）+
 * 公开列表（仅 approved，绝不含 email）/ 后台全量、回复、上下架、删除。
 * 限流窗口与落库时间均取自注入 Clock，测试可拨钟。
 */
@Service
public class GuestMessageService {

    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_HIDDEN = "hidden";

    private static final int MAX_PER_MINUTE = 3;
    private static final int MAX_PUBLIC_LIMIT = 100;
    private static final int MAX_ADMIN_LIST = 500;

    private final GuestMessageMapper guestMessageMapper;
    private final Clock clock;

    public GuestMessageService(GuestMessageMapper guestMessageMapper, Clock clock) {
        this.guestMessageMapper = guestMessageMapper;
        this.clock = clock;
    }

    /** 提交留言：默认 status=approved 自动通过；同 IP 每分钟第 4 条起 429。 */
    public MessageDTO create(MessageCreateRequest request, String ip) {
        LocalDateTime now = LocalDateTime.now(clock);
        Long recent = guestMessageMapper.selectCount(new LambdaQueryWrapper<GuestMessage>()
                .eq(GuestMessage::getIp, ip)
                .ge(GuestMessage::getCreatedAt, now.minusMinutes(1)));
        if (recent != null && recent >= MAX_PER_MINUTE) {
            throw new BizException(429, "留言太频繁，请 1 分钟后再试");
        }

        GuestMessage message = new GuestMessage();
        message.setNickname(request.nickname().trim());
        message.setContent(request.content().trim());
        message.setEmail(blankToNull(request.email()));
        message.setStatus(STATUS_APPROVED);
        message.setIp(ip);
        message.setCreatedAt(now);
        guestMessageMapper.insert(message);
        return toDTO(message);
    }

    /** 公开列表：仅 approved，新→旧；limit 默认 20，clamp 到 [1, 100]。 */
    public List<MessageDTO> listApproved(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_PUBLIC_LIMIT);
        return guestMessageMapper.selectList(new LambdaQueryWrapper<GuestMessage>()
                        .eq(GuestMessage::getStatus, STATUS_APPROVED)
                        .orderByDesc(GuestMessage::getId)
                        .last("LIMIT " + safeLimit))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /** 后台列表：全量含状态；status 可选过滤（approved/hidden，空 = 全部）。 */
    public List<AdminMessageDTO> listAdmin(String status) {
        LambdaQueryWrapper<GuestMessage> wrapper = new LambdaQueryWrapper<GuestMessage>()
                .orderByDesc(GuestMessage::getId);
        if (status != null && !status.isBlank()) {
            if (!STATUS_APPROVED.equals(status) && !STATUS_HIDDEN.equals(status)) {
                throw new BizException(400, "status 必须是 approved/hidden");
            }
            wrapper.eq(GuestMessage::getStatus, status);
        }
        return guestMessageMapper.selectList(wrapper.last("LIMIT " + MAX_ADMIN_LIST))
                .stream()
                .map(this::toAdminDTO)
                .toList();
    }

    /** 回复：reply 空 = 清除回复（replied_at 一并清空）；留言不存在 404。 */
    public void reply(long id, String reply) {
        requireMessage(id);
        String normalized = blankToNull(reply);
        GuestMessage update = new GuestMessage();
        update.setId(id);
        update.setReply(normalized);
        update.setRepliedAt(normalized == null ? null : LocalDateTime.now(clock));
        guestMessageMapper.updateById(update);
    }

    /** 上下架：approved / hidden；留言不存在 404。 */
    public void updateStatus(long id, String status) {
        requireMessage(id);
        GuestMessage update = new GuestMessage();
        update.setId(id);
        update.setStatus(status);
        guestMessageMapper.updateById(update);
    }

    /** 删除（物理删除，保留策略不适用留言表）；留言不存在 404。 */
    public void delete(long id) {
        requireMessage(id);
        guestMessageMapper.deleteById(id);
    }

    private GuestMessage requireMessage(long id) {
        GuestMessage message = guestMessageMapper.selectById(id);
        if (message == null) {
            throw new BizException(404, "留言不存在: " + id);
        }
        return message;
    }

    private MessageDTO toDTO(GuestMessage message) {
        return new MessageDTO(
                message.getId(),
                message.getNickname(),
                message.getContent(),
                message.getReply(),
                message.getRepliedAt(),
                message.getCreatedAt());
    }

    private AdminMessageDTO toAdminDTO(GuestMessage message) {
        return new AdminMessageDTO(
                message.getId(),
                message.getNickname(),
                message.getEmail(),
                message.getContent(),
                message.getStatus(),
                message.getReply(),
                message.getRepliedAt(),
                IpMasker.mask(message.getIp()),
                message.getCreatedAt());
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
