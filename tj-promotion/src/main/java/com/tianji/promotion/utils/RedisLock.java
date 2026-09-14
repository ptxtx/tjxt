package com.tianji.promotion.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class RedisLock {
    private final String key;
    private final StringRedisTemplate redisTemplate;
    public boolean tryLock(long leaseTime, TimeUnit unit){
        //获取线程名称
        String value = Thread.currentThread().getName();
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, value, leaseTime, unit);
        return success != null && success;
    }
    public void unlock(){
        redisTemplate.delete(key);
    }
}
