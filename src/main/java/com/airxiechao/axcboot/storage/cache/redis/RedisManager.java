package com.airxiechao.axcboot.storage.cache.redis;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RedisManager {
    private static RedisManager ourInstance = new RedisManager();

    public static RedisManager getInstance() {
        return ourInstance;
    }

    private RedisManager() {
    }

    private Map<String, Redis> redisMap = new ConcurrentHashMap<>();

    public Redis createRedis(String name, String ip, int port, String password, int maxPoolSize){
        Redis cache = new Redis(name, ip, port, password, maxPoolSize);
        redisMap.put(name, cache);
        return cache;
    }

    public Redis getRedis(String name){
        return redisMap.get(name);
    }

    public void clear(){
        redisMap.forEach((name, redis) -> redis.close());
        redisMap.clear();
    }
}
