package com.whoami.module.oplog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.whoami.module.oplog.entity.AdminOpLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminOpLogMapper extends BaseMapper<AdminOpLog> {
}
