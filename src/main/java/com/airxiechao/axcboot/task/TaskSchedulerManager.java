package com.airxiechao.axcboot.task;

import com.airxiechao.axcboot.task.quartz.QuartzScheduler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TaskSchedulerManager {
    private static TaskSchedulerManager instance = new TaskSchedulerManager();

    public static TaskSchedulerManager getInstance() {
        return instance;
    }

    private static final String QUARTZ_SCHEDULER = "QUARTZ_SCHEDULER";

    private TaskSchedulerManager() {
    }

    private Map<String, ITaskScheduler> taskSchedulers = new ConcurrentHashMap<>();

    private ITaskScheduler createTaskScheduler(String name, int corePoolSize){
        TaskScheduler scheduler = new TaskScheduler(name, corePoolSize);
        taskSchedulers.put(name, scheduler);
        return scheduler;
    }

    public ITaskScheduler getTaskScheduler(String name, int corePoolSize){
        ITaskScheduler scheduler = taskSchedulers.get(name);
        if(null == scheduler){
            scheduler = createTaskScheduler(name, corePoolSize);
        }

        return scheduler;
    }

    public ITaskScheduler getQuartzScheduler(){
        ITaskScheduler scheduler = taskSchedulers.get(QUARTZ_SCHEDULER);
        if(null == scheduler){
            scheduler = new QuartzScheduler();
            taskSchedulers.put(QUARTZ_SCHEDULER, scheduler);
        }

        return scheduler;
    }

    public void shutdownAll(){
        taskSchedulers.forEach((name, scheduler) -> scheduler.shutdownNow());
        taskSchedulers.clear();
    }

    public void shutdownGracefullyAll(){
        taskSchedulers.forEach((name, scheduler) -> scheduler.shutdownGracefully());
        taskSchedulers.clear();
    }
}
