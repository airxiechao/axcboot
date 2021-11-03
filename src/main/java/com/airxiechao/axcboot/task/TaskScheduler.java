package com.airxiechao.axcboot.task;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.*;

public class TaskScheduler implements ITaskScheduler{

    private static final int DAY_SECS = 60*60*24;

    private ScheduledExecutorService executorService;

    public TaskScheduler(String name, int corePoolSize) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("schedule-task-["+name+"]-%d")
                .setDaemon(true)
                .build();

        executorService = new ScheduledThreadPoolExecutor(corePoolSize, threadFactory);
    }

    @Override
    public ScheduledFuture<?> scheduleOnceAfter(int delay, TimeUnit timeUnit, Runnable runnable){
        return executorService.schedule(runnable, delay, timeUnit);
    }

    @Override
    public ScheduledFuture<?> scheduleOnceAt(int startHour, int startMinute, int startSecond, Runnable runnable){
        long initDelaySec = calInitDelaySec(startHour, startMinute, startSecond);

        return executorService.schedule(runnable, initDelaySec, TimeUnit.SECONDS);
    }



    @Override
    public ScheduledFuture<?> schedulePeriodAfter(int delay, int period, TimeUnit timeUnit, Runnable runnable){
        return executorService.scheduleAtFixedRate(runnable, delay, period, timeUnit);
    }

    @Override
    public ScheduledFuture<?> schedulePeriodAt(int startHour, int startMinute, int startSecond,
                                               int period, TimeUnit timeUnit, Runnable runnable){
        long initDelaySec = calInitDelaySec(startHour, startMinute, startSecond);
        long periodSec = timeUnit.toSeconds(period);

        return executorService.scheduleAtFixedRate(runnable, initDelaySec, periodSec, TimeUnit.SECONDS);
    }

    @Override
    public void shutdownNow(){
        executorService.shutdownNow();
    }

    @Override
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
