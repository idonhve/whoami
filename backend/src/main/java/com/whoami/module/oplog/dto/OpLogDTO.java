package com.whoami.module.oplog.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

/** 操作日志查询返回项，detail 为脱敏后的参数摘要（JSON） */
public record OpLogDTO(
        Long id,
        Long adminUserId,
        String action,
        String resource,
        String resourceId,
        JsonNode detail,
        String ip,
        LocalDateTime createdAt) {
}
