package com.airxiechao.axcboot.task.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class QuartzRunnableJob implements Job {

    public static final String RUNNABLE = "RUNNABLE";
    public static final String FUTURE = "FUTURE";

    public QuartzRunnableJob() {
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Runnable runnable = (Runnable) context.getMergedJobDataMap().get(RUNNABLE);
        QuartzScheduledFuture future = (QuartzScheduledFuture)context.getMergedJobDataMap().get(FUTURE);

        try {
            runnable.run();
            future.success(null);
        } catch (Exception e) {
            future.fail(e);
        }
    }
}