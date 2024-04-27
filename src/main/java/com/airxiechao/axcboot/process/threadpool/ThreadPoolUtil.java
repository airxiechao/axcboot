package com.airxiechao.axcboot.process.threadpool;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolUtil {

    public static void shutdownNow(ThreadPoolExecutor executor){
        executor.shutdownNow();
    }

    public static void shutdownGracefully(ThreadPoolExecutor executor, long timeout, TimeUnit unit){
        executor.shutdown();
        try {
            executor.awaitTermination(timeout, unit);
        } catch (InterruptedException e) {
        }
        executor.shutdownNow();
    }
}
