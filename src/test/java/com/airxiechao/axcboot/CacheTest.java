package com.airxiechao.axcboot;

import com.airxiechao.axcboot.storage.annotation.Table;
import com.airxiechao.axcboot.storage.cache.expire.ExpiringCache;
import com.airxiechao.axcboot.storage.cache.expire.ExpiringCacheManager;
import com.airxiechao.axcboot.storage.cache.memory.MemoryCache;
import com.airxiechao.axcboot.storage.cache.memory.MemoryCacheManager;
import com.airxiechao.axcboot.util.TimeUtil;

import java.util.Date;

public class CacheTest {

    public static void main(String[] args) throws InterruptedException {

        ExpiringCacheManager.getInstance().createCache("expire", 5, ExpiringCache.UNIT.SECOND);
        ExpiringCache<Integer> expiringCache = ExpiringCacheManager.getInstance().getCache("expire");

        new Thread(()->{
            while (true){
                int newValue = ((Long)(new Date().getTime() % 10)).intValue();

                System.out.println("----------- "+ TimeUtil.toTimeStr(new Date()) + " put " + newValue);
                expiringCache.put("key"+newValue, newValue);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }

        }).start();

        new Thread(()->{
            while (true){
                System.out.println("??????????? " + TimeUtil.toTimeStr(new Date()) + " get : " + expiringCache.get("key0"));

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }

        }).start();


        while (true){
            Thread.sleep(1000);
        }
    }
}