package com.airxiechao.axcboot.storage.cache.persistence;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PersistenceCacheManager {
    private static PersistenceCacheManager ourInstance = new PersistenceCacheManager();
    public static PersistenceCacheManager getInstance() {
        return ourInstance;
    }
    private PersistenceCacheManager() {
    }

    private Map<String, PersistenceCache> caches = new ConcurrentHashMap<>();

    private <K> PersistenceCache<K> createCache(String fileName){
        PersistenceCache<K> cache = new PersistenceCache<>(fileName);
        caches.put(fileName, cache);
        return cache;
    }

    public <K,V> PersistenceCache<K> getCache(String fileName){
        PersistenceCache<K> cache = this.caches.get(fileName);
        if(null == cache){
            cache = createCache(fileName);
        }

        return cache;
    }

    public void destroy(String fileName){
        PersistenceCache cache = this.caches.get(fileName);
        if(null != cache){
            cache.close();
            this.caches.remove(fileName);
        }
    }

    public void destroyAll(){
        this.caches.forEach((s, persistenceCache) -> persistenceCache.close());
        this.caches.clear();
    }
}
