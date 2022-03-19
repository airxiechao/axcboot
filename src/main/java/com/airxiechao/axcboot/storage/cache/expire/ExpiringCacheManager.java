package com.airxiechao.axcboot.storage.cache.expire;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExpiringCacheManager {
    private static ExpiringCacheManager ourInstance = new ExpiringCacheManager();

    public static ExpiringCacheManager getInstance() {
        return ourInstance;
    }

    private ExpiringCacheManager() {
    }

    private static Map<String, ExpiringCache> caches = new ConcurrentHashMap<>();

    public <T> ExpiringCache<T> createCache(String name, int expirePeriod, ExpiringCache.UNIT unit){
        ExpiringCache<T> cache = new ExpiringCache<>(name, expirePeriod, unit);
        caches.put(name, cache);
        return cache;
    }

    public <T> ExpiringCache<T> getCache(String name){
        return caches.get(name);
    }

    public <T> ExpiringCache<T> getCache(String name, int expirePeriod, ExpiringCache.UNIT unit){
        if(caches.containsKey(name)){
            return caches.get(name);
        }else{
            return createCache(name, expirePeriod, unit);
        }
    }
}
