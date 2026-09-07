package com.tianji.learning.utils;

import java.time.Duration;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class DelayTask<D> implements Delayed {
    private D data;
    private long deadlineNanos;

    public D getData() {
        return data;
    }

    public DelayTask(D data, Duration delayTime) {
        this.data = data;
        this.deadlineNanos = System.nanoTime()+delayTime.toNanos();
    }


    @Override
    public long getDelay(TimeUnit unit) {//获取当前任务剩余延时时间
        return unit.convert(Math.max(0, deadlineNanos - System.nanoTime()), TimeUnit.NANOSECONDS);//将来-现在=剩余;且单位转换
    }

    @Override
    public int compareTo(Delayed o) {
        long l = getDelay(TimeUnit.NANOSECONDS) - o.getDelay(TimeUnit.NANOSECONDS);//当前任务对象-指定任务对象
        return l == 0 ? 0 : (l < 0 ? -1 : 1);
    }
}
