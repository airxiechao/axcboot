package com.airxiechao.axcboot.task.quartz;

import com.airxiechao.axcboot.task.ITaskScheduler;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class QuartzScheduler implements ITaskScheduler {

    private static final Logger logger = LoggerFactory.getLogger(QuartzScheduler.class);

    private Scheduler scheduler;

    public QuartzScheduler() {
        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
        } catch (SchedulerException e) {
            logger.error("start quartz scheduler error", e);
            throw new RuntimeException(e);
        }
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public ScheduledFuture<?> scheduleOnceAfter(int delay, TimeUnit timeUnit, Runnable runnable) {

        // job
        QuartzScheduledFuture future = new QuartzScheduledFuture(scheduler);
        JobDetail job = createJobDetail(runnable, future);
        future.setJob(job);

        // trigger
        Trigger trigger = createOnceTriggerAfter(delay, timeUnit);

        try {
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            logger.error("schedule quartz job error", e);
            throw new RuntimeException(e);
        }

        return future;
    }

    @Override
    public ScheduledFuture<?> scheduleOnceAt(int startHour, int startMinute, int startSecond, Runnable runnable) {

        // job
        QuartzScheduledFuture future = new QuartzScheduledFuture(scheduler);
        JobDetail job = createJobDetail(runnable, future);
        future.setJob(job);

        // trigger
        Trigger trigger = createOnceTriggerAt(startHour, startMinute, startSecond);

        try {
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            logger.error("schedule quartz job error", e);
            throw new RuntimeException(e);
        }

        return future;
    }

    @Override
    public ScheduledFuture<?> schedulePeriodAfter(int delay, int period, TimeUnit timeUnit, Runnable runnable) {
        // job
        QuartzScheduledFuture future = new QuartzScheduledFuture(scheduler);
        future.success(null);
        JobDetail job = createJobDetail(runnable, future);
        future.setJob(job);

        // trigger
        Trigger trigger = createPeriodTriggerAfter(delay, period, timeUnit);

        try {
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            logger.error("schedule quartz job error", e);
            throw new RuntimeException(e);
        }

        return future;

    }

    @Override
    public ScheduledFuture<?> schedulePeriodAt(int startHour, int startMinute, int startSecond,
                                               int period, TimeUnit timeUnit, Runnable runnable) {
        // job
        QuartzScheduledFuture future = new QuartzScheduledFuture(scheduler);
        future.success(null);
        JobDetail job = createJobDetail(runnable, future);
        future.setJob(job);

        // trigger
        Trigger trigger = createPeriodTriggerAt(startHour, startMinute, startSecond,
                period, timeUnit);

        try {
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            logger.error("schedule quartz job error", e);
            throw new RuntimeException(e);
        }

        return future;
    }


    @Override
    public void shutdownNow() {
        try {
            scheduler.shutdown();
        } catch (SchedulerException e) {
            logger.error("shutdown quartz scheduler error", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void shutdownGracefully() {
        try {
            scheduler.shutdown();
        } catch (SchedulerException e) {
            logger.error("shutdown quartz scheduler error", e);
            throw new RuntimeException(e);
        }
    }

    private JobDetail createJobDetail(Runnable runnable, QuartzScheduledFuture future){
        JobDetail job = newJob(QuartzRunnableJob.class).build();
        job.getJobDataMap().put(QuartzRunnableJob.RUNNABLE, runnable);
        job.getJobDataMap().put(QuartzRunnableJob.FUTURE, future);
        return job;
    }

    private Trigger createOnceTriggerAfter(int delay, TimeUnit timeUnit){
        Date startTime = createStartTime(delay, timeUnit);
        Trigger trigger = newTrigger()
                .startAt(startTime)
                .withSchedule(simpleSchedule().withRepeatCount(0))
                .build();

        return trigger;
    }

    private Trigger createOnceTriggerAt(int startHour, int startMinute, int startSecond){
        Date startTime = createStartTime(startHour, startMinute, startSecond);
        Trigger trigger = newTrigger()
                .startAt(startTime)
                .withSchedule(simpleSchedule().withRepeatCount(0))
                .build();

        return trigger;
    }

    private Trigger createPeriodTriggerAfter(int delay, int period, TimeUnit timeUnit){
        Date startTime = createStartTime(delay, timeUnit);
        int periodSecs = (int)timeUnit.toSeconds(period);

        Trigger trigger = newTrigger()
                .startAt(startTime)
                .withSchedule(simpleSchedule().withIntervalInSeconds(periodSecs).repeatForever())
                .build();

        return trigger;
    }

    private Trigger createPeriodTriggerAt(int startHour, int startMinute, int startSecond,
                                        int period, TimeUnit timeUnit){
        Date startTime = createStartTime(startHour, startMinute, startSecond);
        int periodSecs = (int)timeUnit.toSeconds(period);

        Trigger trigger = newTrigger()
                .startAt(startTime)
                .withSchedule(simpleSchedule().withIntervalInSeconds(periodSecs).repeatForever())
                .build();

        return trigger;
    }

    private Date createStartTime(int delay, TimeUnit timeUnit){
            long delaySeconds = timeUnit.toSeconds(delay);
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            cal.add(Calendar.SECOND, (int)delaySeconds);
            return cal.getTime();
    }

    private Date createStartTime(int startHour, int startMinute, int startSecond){
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.set(Calendar.HOUR_OF_DAY, startHour);
        cal.set(Calendar.MINUTE, startMinute);
        cal.set(Calendar.SECOND, startSecond);
        cal.set(Calendar.MILLISECOND, 0);
        if(!now.before(cal.getTime())){
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        return cal.getTime();
    }
}
