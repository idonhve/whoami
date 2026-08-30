package com.whoami.module.oplog.controller;

import com.whoami.common.ApiResult;
import com.whoami.module.oplog.dto.OpLogPage;
import com.whoami.module.oplog.service.OpLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/op-logs")
public class OpLogController {

    private static final int MAX_PAGE_SIZE = 100;

    private final OpLogService opLogService;

    public OpLogController(OpLogService opLogService) {
        this.opLogService = opLogService;
    }

    @GetMapping
    public ApiResult<OpLogPage> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return ApiResult.ok(opLogService.page(safePage, safeSize));
    }
}
