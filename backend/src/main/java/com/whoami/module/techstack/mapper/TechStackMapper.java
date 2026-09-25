package com.whoami.module.techstack.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.whoami.module.techstack.entity.TechStack;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TechStackMapper extends BaseMapper<TechStack> {
}