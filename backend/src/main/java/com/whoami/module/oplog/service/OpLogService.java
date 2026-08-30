package com.whoami.module.oplog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.whoami.module.oplog.dto.OpLogDTO;
import com.whoami.module.oplog.dto.OpLogPage;
import com.whoami.module.oplog.entity.AdminOpLog;
import com.whoami.module.oplog.mapper.AdminOpLogMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OpLogService {

    private static final Logger log = LoggerFactory.getLogger(OpLogService.class);

    private final AdminOpLogMapper adminOpLogMapper;
    private final ObjectMapper objectMapper;

    public OpLogService(AdminOpLogMapper adminOpLogMapper, ObjectMapper objectMapper) {
        this.adminOpLogMapper = adminOpLogMapper;
        this.objectMapper = objectMapper;
    }

    /** 写入一条操作日志（由 OpLogAspect 调用，调用方已兜底异常） */
    public void record(AdminOpLog entry) {
        adminOpLogMapper.insert(entry);
    }

    /** 倒序分页查询 */
    public OpLogPage page(int page, int size) {
        Page<AdminOpLog> result = adminOpLogMapper.selectPage(
                Page.of(page, size),
                new LambdaQueryWrapper<AdminOpLog>().orderByDesc(AdminOpLog::getId));
        List<OpLogDTO> list = result.getRecords().stream().map(this::toDTO).toList();
        return new OpLogPage(list, result.getTotal());
    }

    private OpLogDTO toDTO(AdminOpLog entity) {
        return new OpLogDTO(
                entity.getId(),
                entity.getAdminUserId(),
                entity.getAction(),
                entity.getResource(),
                entity.getResourceId(),
                parseDetail(entity.getDetail()),
                entity.getIp(),
                entity.getCreatedAt());
    }

    private JsonNode parseDetail(String detail) {
        if (detail == null || detail.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(detail);
        } catch (Exception e) {
            log.warn("admin_op_log.detail 非合法 JSON，按原文返回: {}", e.getMessage());
            return TextNode.valueOf(detail);
        }
    }
}
