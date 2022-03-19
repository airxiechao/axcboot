package com.airxiechao.axcboot;

import com.airxiechao.axcboot.storage.cache.persistence.PersistenceCache;
import com.airxiechao.axcboot.storage.cache.persistence.PersistenceCacheManager;

public class PersistenceCacheTest {

    public static void main(String[] args) throws InterruptedException {

        PersistenceCache<Long> cache = PersistenceCacheManager.getInstance().getCache("temp/data.cache");

        String v = cache.map().get(1L);
        System.out.println(v);

        cache.map().put(1L, "123");
        cache.commit();
    }
}
