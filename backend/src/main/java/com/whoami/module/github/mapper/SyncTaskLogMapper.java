package com.whoami.module.github.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.whoami.module.github.entity.SyncTaskLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SyncTaskLogMapper extends BaseMapper<SyncTaskLog> {
}
