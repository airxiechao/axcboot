package com.airxiechao.axcboot.db;

import com.airxiechao.axcboot.task.ITaskScheduler;
import com.airxiechao.axcboot.task.TaskSchedulerManager;

import java.util.concurrent.TimeUnit;

public class ScheduleTaskTest {

    public static void main(String[] args) throws InterruptedException {
        ITaskScheduler scheduleTask = TaskSchedulerManager.getInstance().getTaskScheduler("scheduler", 1);
        scheduleTask.schedulePeriodAfter(0, 1, TimeUnit.SECONDS, ()->{
            System.out.println("task run");
        });

        while (true){
            Thread.sleep(1000);
        }
    }
}
