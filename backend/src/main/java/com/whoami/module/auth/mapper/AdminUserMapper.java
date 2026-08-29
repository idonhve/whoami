package com.whoami.module.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.whoami.module.auth.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {
}
