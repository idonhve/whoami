package com.whoami.module.guestmessage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.whoami.module.guestmessage.entity.GuestMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GuestMessageMapper extends BaseMapper<GuestMessage> {
}
