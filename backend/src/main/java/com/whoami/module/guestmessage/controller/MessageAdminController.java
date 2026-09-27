package com.whoami.module.guestmessage.controller;

import com.whoami.common.ApiResult;
import com.whoami.module.guestmessage.dto.AdminMessageDTO;
import com.whoami.module.guestmessage.dto.MessageReplyRequest;
import com.whoami.module.guestmessage.dto.MessageStatusRequest;
import com.whoami.module.guestmessage.service.GuestMessageService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 后台留言管理接口（JWT 保护，操作日志由 OpLogAspect 自动记录）。 */
@RestController
@RequestMapping("/admin/api/messages")
public class MessageAdminController {

    private final GuestMessageService guestMessageService;

    public MessageAdminController(GuestMessageService guestMessageService) {
        this.guestMessageService = guestMessageService;
    }

    @GetMapping
    public ApiResult<List<AdminMessageDTO>> list(@RequestParam(required = false) String status) {
        return ApiResult.ok(guestMessageService.listAdmin(status));
    }

    @PutMapping("/{id}/reply")
    public ApiResult<Void> reply(
            @PathVariable("id") long id,
            @Valid @RequestBody MessageReplyRequest request) {
        guestMessageService.reply(id, request.reply());
        return ApiResult.ok();
    }

    @PutMapping("/{id}/status")
    public ApiResult<Void> updateStatus(
            @PathVariable("id") long id,
            @Valid @RequestBody MessageStatusRequest request) {
        guestMessageService.updateStatus(id, request.status());
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable("id") long id) {
        guestMessageService.delete(id);
        return ApiResult.ok();
    }
}
