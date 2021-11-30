package com.gmall.gateway.filter;

import com.alibaba.fastjson.JSONObject;
import com.gmall.common.result.Result;
import com.gmall.common.result.ResultCodeEnum;
import com.gmall.gateway.config.RedisConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Component
public class LoginFilter implements GlobalFilter, Ordered {

    public static final String TOKEN = "token";

    public static final String LOGINURL = "http://passport.gmall.com/login.html?originUrl=";

    public static final String USERTEMPID = "userTempId";
    @Autowired
    private RedisTemplate redisTemplate;

    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Value("${login.url}")
    private String[] loginUrl;
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 进行路径匹配，如果访问的是内部资源则进项拦截
        String path = exchange.getRequest().getURI().getPath();
        String allPath = exchange.getRequest().getURI().toString();

        if (antPathMatcher.match("/**/inner/**", path)) {
            // 访问的是内部资源 进项拦截，返回相应提示
             return returnMesage(exchange, ResultCodeEnum.PERMISSION);
        }
        // 从数据库中获取登录信息
        String userId = getUserId(exchange);
        // 判断访问的资源是否需要登录及当前是否已经登录
        //2.1: 是否登录：异步   未登录   Result对象 （信息 Code 数据）
        if (antPathMatcher.match("/**/auth/**", path) && StringUtils.isEmpty(userId)) {
            return returnMesage(exchange, ResultCodeEnum.LOGIN_AUTH);
        }
        //2.2: 是否登录：同步   未登录  重定向到登录页面
        for (String url : loginUrl) {
            if (path.contains(url) && StringUtils.isEmpty(userId)) {
                // 设置状态码为重定向303
                exchange.getResponse().setStatusCode(HttpStatus.SEE_OTHER);
                // 设置从定向地址
                //重定向的路径
                try {
                    exchange.getResponse().getHeaders().add(HttpHeaders.LOCATION,
                            LOGINURL +  URLEncoder.encode(allPath,"utf-8"));
                    // 跳转到重定向地址
                    return exchange.getResponse().setComplete();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        //3:准备放行的时候   如果不为空或NULL 可以传递用户的ID了  请求头
        if(!StringUtils.isEmpty(userId)){
            //request.getHeaders().add("userId",userId);
            exchange.getRequest().mutate().header("userId", userId);
        }
        //3:准备放行的时候  获取并传递临时Id
        String userTempId = getUserTempId(exchange);
        if(!StringUtils.isEmpty(userTempId)){
            //request.getHeaders().add("userId",userId);
            exchange.getRequest().mutate().header("userTempId", userTempId);
        }
        // 放行
        return chain.filter(exchange);

    }

    private String getUserTempId(ServerWebExchange exchange) {
        String userTempId = exchange.getRequest().getHeaders().getFirst(USERTEMPID);
        if (StringUtils.isEmpty(userTempId)) {
            HttpCookie cookie = exchange.getRequest().getCookies().getFirst(USERTEMPID);
            if (null != cookie) {
                userTempId = cookie.getValue();
            }
        }
        return userTempId;
    }

    private Mono<Void> returnMesage(ServerWebExchange exchange, ResultCodeEnum resultCodeEnum) {

        Result<Object> result = Result.build(null, resultCodeEnum);
        String s = JSONObject.toJSONString(result);
        DataBufferFactory dataBufferFactory = exchange.getResponse().bufferFactory();
        DataBuffer wrap = dataBufferFactory.wrap(s.getBytes());
        return exchange.getResponse().writeWith(Mono.just(wrap));
    }

    private String getUserId(ServerWebExchange exchange) {
        String token = exchange.getRequest().getHeaders().getFirst(TOKEN);
        if (StringUtils.isEmpty(token)) {
            HttpCookie cookie = exchange.getRequest().getCookies().getFirst(TOKEN);
            if (null != cookie) {
                token = cookie.getValue();
            }
        }
        // 获取userId
        if (!StringUtils.isEmpty(token)) {
            String redisToken = RedisConst.USER_LOGIN_KEY_PREFIX
                    + token + RedisConst.userinfoKey_suffix;
            if (redisTemplate.hasKey(redisToken)) {
                return  redisTemplate.opsForValue().get(redisToken).toString();
            }
        }
        return null;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
