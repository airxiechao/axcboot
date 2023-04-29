package com.airxiechao.axcboot.task.quartz;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import java.util.concurrent.*;

public class QuartzScheduledFuture implements ScheduledFuture<Object> {

    private Scheduler scheduler;
    private JobDetail job;
    private Object result;
    private Throwable error;
    private CountDownLatch latch = new CountDownLatch(1);

    public QuartzScheduledFuture(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void setJob(JobDetail job) {
        this.job = job;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if(null != job){
            try {
                return scheduler.deleteJob(job.getKey());
            } catch (SchedulerException e) {
                throw new RuntimeException("cancel quartz job error", e);
            }
        }

        return false;
    }

    @Override
    public boolean isCancelled() {
        if(null != job){
            try {
                return !scheduler.checkExists(job.getKey());
            } catch (SchedulerException e) {
                throw new RuntimeException("check quartz job exists error", e);
            }
        }
        return false;
    }

    @Override
    public boolean isDone() {
        return result != null || error != null;
    }

    public void success(Object result) {
        this.result = result;
        latch.countDown();
    }

    public void fail(Throwable error) {
        this.error = error;
        latch.countDown();
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        latch.await();
        if (null == result || null != error) {
            throw new ExecutionException(error);
        }
        return result;
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean got = latch.await(timeout, unit);
        if (!got) {
            throw new TimeoutException();
        }
        if (null == result || null != error) {
            throw new ExecutionException(error);
        }
        return result;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int compareTo(Delayed o) {
        throw new UnsupportedOperationException();
    }
}