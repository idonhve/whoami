package com.whoami.module.guestmessage.controller;

import com.whoami.common.ApiResult;
import com.whoami.module.guestmessage.dto.MessageCreateRequest;
import com.whoami.module.guestmessage.dto.MessageDTO;
import com.whoami.module.guestmessage.service.GuestMessageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 公开留言接口（免登录）：提交（同 IP 每分钟 ≤ 3 条，超限 429）+ 仅 approved 的列表。 */
@RestController
@RequestMapping("/api/messages")
public class MessagePublicController {

    private final GuestMessageService guestMessageService;

    public MessagePublicController(GuestMessageService guestMessageService) {
        this.guestMessageService = guestMessageService;
    }

    @PostMapping
    public ApiResult<Void> create(
            @Valid @RequestBody MessageCreateRequest request,
            HttpServletRequest httpRequest) {
        guestMessageService.create(request, clientIp(httpRequest));
        return ApiResult.ok();
    }

    @GetMapping
    public ApiResult<List<MessageDTO>> list(@RequestParam(defaultValue = "20") int limit) {
        return ApiResult.ok(guestMessageService.listApproved(limit));
    }

    /** 与 ResumeService / OpLogAspect 相同口径：X-Forwarded-For 首个优先（nginx 反代场景） */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
