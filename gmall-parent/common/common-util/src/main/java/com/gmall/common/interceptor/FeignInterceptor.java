package com.gmall.common.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 李旭
 * @date 2021/11/27 15:54
 * @Description:
 *
 *   远程调用拦截器
 *
 */
@Component
public class FeignInterceptor implements RequestInterceptor {

    //1、办法1： 从SpringIOC容器获取   当前项目中如果没有请求对象  直接报错了
/*    @Autowired(required = false)
    private HttpServletRequest request;*/

    //马上要发出请求的 拦截器执行方法
    @Override
    public void apply(RequestTemplate requestTemplate) {
        //1:当前微服务的请求对象
        /*
         办法2：*/
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();
        if(null != requestAttributes){
            HttpServletRequest request = requestAttributes.getRequest();
            if(null != request){
                String userId = request.getHeader("userId");
                if(!StringUtils.isEmpty(userId)){
                    //2：马上要发出的请求对象
                    requestTemplate.header("userId",userId);
                }
                String userTempId = request.getHeader("userTempId");
                if(!StringUtils.isEmpty(userTempId)){
                    //2：马上要发出的请求对象
                    requestTemplate.header("userTempId",userTempId);
                }
            }
        }

    }
}
