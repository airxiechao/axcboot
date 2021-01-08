package com.airxiechao.axcboot.process.threadpool;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ThreadPoolManager {
    private static ThreadPoolManager ourInstance = new ThreadPoolManager();

    public static ThreadPoolManager getInstance() {
        return ourInstance;
    }

    private Map<String, ThreadPool> pools = new ConcurrentHashMap<>();

    private ThreadPoolManager() {
    }

    public ThreadPool getThreadPool(String name, int corePoolSize, int maxPoolSize, int maxQueueSize){
        ThreadPool pool = pools.get(name);
        if(null != pool){
            return pool;
        }else{
            pool = new ThreadPool(name, corePoolSize, maxPoolSize, maxQueueSize);
            pools.put(name, pool);
            return pool;
        }
    }

    public void shutdownAll(){
        pools.forEach((name, pool) -> pool.shutdownNow());
        pools.clear();
    }

    public void shutdownGracefullyAll(){
        pools.forEach((name, pool) -> pool.shutdownGracefully());
        pools.clear();
    }
}
