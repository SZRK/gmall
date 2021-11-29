package com.gmall.user.controller;

import com.gmall.common.constant.RedisConst;
import com.gmall.common.result.Result;
import com.gmall.model.user.UserInfo;
import com.gmall.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user/passport")
public class UserController {

    // api/user/passport/login
    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping("/login")
    public Result login(@RequestBody UserInfo userInfo) {
        // 判断用户名和密码是否为空那个
        if (StringUtils.isEmpty(userInfo.getLoginName()) || StringUtils.isEmpty(userInfo.getPasswd())) {
            return Result.fail().message("用户名或密码不能为空");
        }
        // 查询数据库，校验用户名和密码
        userInfo = userService.login(userInfo);
        if (null != userInfo) {
            // 用户信息验证通过 生成相应的token令牌保存到redis中
            String token = UUID.randomUUID().toString().replaceAll("-", "");
            String redisToken = RedisConst.USER_LOGIN_KEY_PREFIX
                    + token + RedisConst.userinfoKey_suffix;
            redisTemplate.opsForValue().set(redisToken, userInfo.getId(),
                    RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);
            Map<Object, Object> map = new HashMap<>();
            map.put("token", token);
            map.put("nickName",userInfo.getNickName());// 昵称
            return Result.ok(map);
        }

        return Result.fail().message("用户名或密码输入有误");
    }

    @GetMapping("/logout")
    public Result logout(HttpServletRequest request) {
        String redisToken = RedisConst.USER_LOGIN_KEY_PREFIX +
                request.getHeader("token") + RedisConst.userinfoKey_suffix;
        redisTemplate.delete(redisToken);
        return Result.ok();
    }

}
