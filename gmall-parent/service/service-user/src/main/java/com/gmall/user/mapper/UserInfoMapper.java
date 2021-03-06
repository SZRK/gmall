package com.gmall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gmall.model.user.UserInfo;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {
}
