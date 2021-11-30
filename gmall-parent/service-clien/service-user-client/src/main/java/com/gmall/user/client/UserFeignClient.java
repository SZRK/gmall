package com.gmall.user.client;

import com.gmall.model.user.UserAddress;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient("service-useryc")
public interface UserFeignClient {
    // 获取用户的收货地址
    @GetMapping("/api/user/passport/inner/findUserAddressListByUserId/{userId}")
    public List<UserAddress> getUserAddressList(@PathVariable String userId);
}
