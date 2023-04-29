package com.airxiechao.axcboot.process.threadpool;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPool {

    private ThreadPoolExecutor executor;

    public ThreadPool(String name, int corePoolSize, int maxPoolSize, int maxQueueSize){
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat(name + "-threadpool-%d")
                .setDaemon(true)
                .build();
        executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(maxQueueSize), namedThreadFactory);
    }

    public ThreadPoolExecutor getExecutor() {
        return executor;
    }

    public void shutdownNow(){
        ThreadPoolUtil.shutdownNow(this.executor);
    }

    public void shutdownGracefully(){
        ThreadPoolUtil.shutdownGracefully(this.executor, 10, TimeUnit.SECONDS);
    }
}
