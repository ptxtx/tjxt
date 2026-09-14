package com.tianji.promotion.utils;

import org.redisson.api.RLock;

public enum MyLockStrategy {
    SKIP_FAST(){
        @Override
        public boolean tryLock(RLock lock, MyLock prop) throws InterruptedException {
            boolean b = lock.tryLock(0, prop.leaseTime(), prop.unit());
            return b;
        }
    },
    FAIL_FAST(){
        @Override
        public boolean tryLock(RLock lock, MyLock prop) throws InterruptedException {
            boolean isLock = lock.tryLock(0, prop.leaseTime(), prop.unit());
            if(!isLock){
                throw new RuntimeException("请求太频繁");
            }
            return true;
        }
    },
    KEEP_TRYING(){
        @Override
        public boolean tryLock(RLock lock, MyLock prop) {
           lock.lock( prop.leaseTime(), prop.unit());
           return true;
        }
    },
    SKIP_AFTER_RETRY_TIMEOUT(){
        @Override
        public boolean tryLock(RLock lock, MyLock prop) throws InterruptedException {
            boolean isLock = lock.tryLock(prop.waitTime(), prop.leaseTime(), prop.unit());
            return isLock;
        }
    },
    FAIL_AFTER_RETRY_TIMEOUT(){
        @Override
        public boolean tryLock(RLock lock, MyLock prop) throws InterruptedException {
            boolean isLock = lock.tryLock(prop.waitTime(), prop.leaseTime(), prop.unit());
            if(!isLock){
                throw new RuntimeException("请求太频繁");
            }
            return true;
        }
    },
    ;

    abstract boolean tryLock(RLock lock,MyLock prop) throws InterruptedException;
}
