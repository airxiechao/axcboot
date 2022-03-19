package com.airxiechao.axcboot.storage.cache.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryCache<K,V> {

    private static final Logger logger = LoggerFactory.getLogger(MemoryCache.class);

    private String cacheName;

    public MemoryCache(String cacheName){
        this.cacheName = cacheName;
    }

    private Map<K, V> dataMap = new ConcurrentHashMap<>();

    public Map<K, V> map(){
        return this.dataMap;
    }
}
