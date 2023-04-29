package com.airxiechao.axcboot.storage.cache.memory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryCacheManager {
    private static MemoryCacheManager ourInstance = new MemoryCacheManager();

    public static MemoryCacheManager getInstance() {
        return ourInstance;
    }

    private MemoryCacheManager() {
    }

    private Map<String, MemoryCache> caches = new ConcurrentHashMap<>();

    private <K,V> MemoryCache<K,V> createCache(String name){
        MemoryCache<K,V> cache = new MemoryCache<>(name);
        caches.put(name, cache);
        return cache;
    }

    public <K,V> MemoryCache<K,V> getCache(String name){
        MemoryCache<K,V> memoryCache = caches.get(name);
        if(null == memoryCache){
            memoryCache = createCache(name);
        }

        return memoryCache;
    }
}
