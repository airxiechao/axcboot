package com.airxiechao.axcboot.db;

import com.airxiechao.axcboot.task.ScheduleTask;
import com.airxiechao.axcboot.task.ScheduleTaskManager;

import java.util.concurrent.TimeUnit;

public class ScheduleTaskTest {

    public static void main(String[] args) throws InterruptedException {
        ScheduleTask scheduleTask = ScheduleTaskManager.getInstance().getScheduleTask("scheduler", 1);
        scheduleTask.shceduleEveryPeriod(1, TimeUnit.SECONDS, ()->{
            System.out.println("task run");
        });

        while (true){
            Thread.sleep(1000);
        }
    }
}
