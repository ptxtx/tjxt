package com.tianji.promotion.utils;

import com.tianji.common.exceptions.BizIllegalException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.Ordered;

import java.util.Objects;

@Aspect
@RequiredArgsConstructor
public class MyLockAspect implements Ordered {
    private final RedissonClient redissonClient;
    @Around("@annotation(myLock)")
    public Object tryLock(ProceedingJoinPoint pjp, MyLock myLock) throws Throwable{
        //获取锁对象
        RLock lock=null;
        switch(myLock.lockType()) {
            case RE_ENTRANT_LOCK:
                lock = redissonClient.getLock(myLock.name());
                break;
            case READ_LOCK:
                lock = redissonClient.getReadWriteLock(myLock.name()).readLock();
                break;
            case WRITE_LOCK:
                lock = redissonClient.getReadWriteLock(myLock.name()).writeLock();
                break;
            case FAIR_LOCK:
                lock = redissonClient.getFairLock(myLock.name());
                break;
            default:
               throw new BizIllegalException("锁类型不存在");
        }
        //尝试获取锁
        boolean isLock =myLock.lockStrategy().tryLock(lock,myLock);
        //判断是否成功
        //失败 抛异常
        if(!isLock){
           // throw new BizIllegalException("请求太频繁");
            return null;
        }
        try{
            return pjp.proceed();
        }finally{
            lock.unlock();
        }

    }

    @Override
    public int getOrder() {//值越大 优先级越小 默认值为int最大值
        return 0;
    }
}
