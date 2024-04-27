package com.airxiechao.axcboot.task;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public interface ITaskScheduler {
    /**
     * 延迟运行一次
     */
    ScheduledFuture<?> scheduleOnceAfter(int delay, TimeUnit timeUnit, Runnable runnable);

    ScheduledFuture<?> scheduleOnceAt(int startHour, int startMinute, int startSecond, Runnable runnable);

    /**
     * 启动周期定时任务
     */
    ScheduledFuture<?> schedulePeriodAfter(int delay, int period, TimeUnit timeUnit, Runnable runnable);

    ScheduledFuture<?> schedulePeriodAt(int startHour, int startMinute, int startSecond,
                                        int period, TimeUnit timeUnit, Runnable runnable);


    /**
     * 关闭
     */
    void shutdownNow();

    void shutdownGracefully();
}
