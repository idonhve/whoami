package com.whoami.module.oplog.dto;

import java.util.List;

/** GET /admin/api/op-logs 分页响应 */
public record OpLogPage(List<OpLogDTO> list, long total) {
}
