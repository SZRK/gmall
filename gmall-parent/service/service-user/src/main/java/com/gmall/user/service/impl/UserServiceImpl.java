package com.gmall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gmall.common.util.MD5;
import com.gmall.model.user.UserAddress;
import com.gmall.model.user.UserInfo;
import com.gmall.user.mapper.UserAddressMapper;
import com.gmall.user.mapper.UserInfoMapper;
import com.gmall.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Override
    public UserInfo login(UserInfo userInfo) {
        return userInfoMapper.selectOne(new QueryWrapper<UserInfo>()
                .eq("login_name", userInfo.getLoginName())
                .eq("passwd", MD5.encrypt(userInfo.getPasswd())));
    }

    @Override
    public List<UserAddress> getUserAddressList(String userId) {
      return  userAddressMapper.selectList(new QueryWrapper<UserAddress>()
                .eq("user_id", userId));
    }
}
