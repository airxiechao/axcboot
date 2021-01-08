package com.airxiechao.axcboot.task;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.*;

public class ScheduleTask {

    private static final int DAY_SECS = 60*60*24;

    private ScheduledExecutorService executorService;

    public ScheduleTask(String name, int corePoolSize) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("schedule-task-["+name+"]-%d")
                .setDaemon(true)
                .build();

        executorService = new ScheduledThreadPoolExecutor(corePoolSize, threadFactory);
    }

    /**
     * 延迟运行一次
     * @param num
     * @param timeUnit
     * @param runnable
     * @return
     */
    public ScheduledFuture<?> scheduleAfter(int num, TimeUnit timeUnit, Runnable runnable){
        return executorService.schedule(runnable, num, timeUnit);
    }

    /**
     * 启动定时任务
     * @param startHour
     * @param startMinute
     * @param startSecond
     * @param periodSecond
     * @param runNow
     * @param runnable
     */
    public ScheduledFuture<?> scheduleEverySecs(int startHour, int startMinute, int startSecond, int periodSecond, boolean runNow, Runnable runnable){
        long initDelaySec = runNow ? 0 : calInitDelaySec(startHour, startMinute, startSecond);

        return executorService.scheduleAtFixedRate(runnable, initDelaySec, periodSecond, TimeUnit.SECONDS);
    }

    /**
     * 启动每日定时任务
     * @param startHour
     * @param startMinute
     * @param startSecond
     * @param runNow
     * @param runnable
     */
    public ScheduledFuture<?> scheduleEveryDay(int startHour, int startMinute, int startSecond, boolean runNow, Runnable runnable){
        long initDelaySec = runNow ? 0 : calInitDelaySec(startHour, startMinute, startSecond);

        return executorService.scheduleAtFixedRate(runnable, initDelaySec, DAY_SECS, TimeUnit.SECONDS);
    }

    /**
     * 启动周期定时任务
     * @param num
     * @param timeUnit
     * @param runnable
     */
    public ScheduledFuture<?> shceduleEveryPeriod(int num, TimeUnit timeUnit, Runnable runnable){
        return executorService.scheduleAtFixedRate(runnable, 0, num, timeUnit);
    }


    /**
     * 关闭
     */
    public void shutdownNow(){
        executorService.shutdownNow();
    }

    public void shutdownGracefully() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        executorService.shutdownNow();
    }

    /**
     * 计算初始延迟时间
     * @param hour
     * @param minute
     * @param second
     * @return
     */
    private static long calInitDelaySec(int hour, int minute, int second){
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        cal.set(Calendar.MILLISECOND, 0);
        if(!now.before(cal.getTime())){
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        long delay = (cal.getTime().getTime() - now.getTime()) / 1000;

        return delay;
    }
}
