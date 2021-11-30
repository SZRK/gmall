package com.gmall.user.service;

import com.gmall.model.user.UserAddress;
import com.gmall.model.user.UserInfo;

import java.util.List;

public interface UserService {
    UserInfo login(UserInfo userInfo);

    List<UserAddress> getUserAddressList(String userId);
}
