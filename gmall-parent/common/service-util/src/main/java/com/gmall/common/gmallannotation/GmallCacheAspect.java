package com.gmall.common.gmallannotation;

import com.gmall.common.constant.RedisConst;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁 + Aop 切面实现类
 * 1、创建自定义注解
 * 2、编写切面类  实现类自定义注解的逻辑缓存
 */
@Component
@Aspect
@Slf4j
public class GmallCacheAspect {

    // 注入相关属性
    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisTemplate redisTemplate;

    // 指定注解切入点表达式
    @Around("@annotation(com.gmall.common.gmallannotation.GmallCache)")
    public Object cacheData(ProceedingJoinPoint pjp) {
        // 获取方法签名
        MethodSignature methodSignature = (MethodSignature) pjp.getSignature();

        // 获取方法参数
        Object[] args = pjp.getArgs();
        // 获取注解信息
        GmallCache gmallCacheAnnotation = methodSignature.getMethod().getAnnotation(GmallCache.class);
        // 获取相应的缓存Key
        String cacheKey = gmallCacheAnnotation.prefix() + Arrays.asList(args) + RedisConst.SKUKEY_SUFFIX + "YC";
        String lockKey = gmallCacheAnnotation.prefix() + Arrays.asList(args) + RedisConst.SKULOCK_SUFFIX + "YC";
        // 获取返回值类型
        Class returnType = methodSignature.getReturnType();

        // 获取缓存中的数据
        Object result = redisTemplate.opsForValue().get(cacheKey);
        if (null == result) {
            // 缓存中没有数据, 从数据库中获取，为了防止缓存击穿，需要加分布式锁
            RLock lock = redissonClient.getLock(lockKey);
            try {
                if (lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS)) {
                    // 上锁成功, 开始查数据库
                    try {
                        result = pjp.proceed();
                        if (null != result) {
                            // 放入缓存中, 并设置差异过期时间
                            redisTemplate.opsForValue().set(cacheKey, result,
                                    RedisConst.SKUKEY_TIMEOUT + new Random().nextInt(300), TimeUnit.SECONDS);
                        } else {
                            // 防止缓存穿透,缓存空值
                            redisTemplate.opsForValue().set(cacheKey, returnType.newInstance(),
                                    RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                        }
                    } finally {
                        lock.unlock();
                    }
                } else {
                    // 没有获取到锁
                    try { TimeUnit.SECONDS.sleep(2); } catch (InterruptedException ex) { ex.printStackTrace(); }
                    result =  redisTemplate.opsForValue().get(cacheKey);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        return result;

    }
}
