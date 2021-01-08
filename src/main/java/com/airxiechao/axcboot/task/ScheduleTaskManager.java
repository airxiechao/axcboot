package com.airxiechao.axcboot.task;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ScheduleTaskManager {
    private static ScheduleTaskManager ourInstance = new ScheduleTaskManager();

    public static ScheduleTaskManager getInstance() {
        return ourInstance;
    }

    private ScheduleTaskManager() {
    }

    private Map<String, ScheduleTask> tasks = new ConcurrentHashMap<>();

    private ScheduleTask createScheduleTask(String name, int corePoolSize){
        ScheduleTask task = new ScheduleTask(name, corePoolSize);
        tasks.put(name, task);
        return task;
    }

    public ScheduleTask getScheduleTask(String name, int corePoolSize){
        ScheduleTask task = tasks.get(name);
        if(null == task){
            task = createScheduleTask(name, corePoolSize);
        }

        return task;
    }

    public void shutdownAll(){
        tasks.forEach((name, task) -> task.shutdownNow());
        tasks.clear();
    }

    public void shutdownGracefullyAll(){
        tasks.forEach((name, task) -> task.shutdownGracefully());
        tasks.clear();
    }
}
