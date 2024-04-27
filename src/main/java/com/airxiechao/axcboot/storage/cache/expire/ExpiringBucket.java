package com.airxiechao.axcboot.storage.cache.expire;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据桶
 */
public class ExpiringBucket<T> {

    private Date expireDateBegin;
    private Date expireDateEnd;
    private Map<String, T> data;

    public ExpiringBucket(Date expireDateBegin, Date expireDateEnd){
        this.expireDateBegin = expireDateBegin;
        this.expireDateEnd = expireDateEnd;
        this.data = new ConcurrentHashMap<>();
    }

    public boolean containsExpireDate(Date expireDate){
        if(expireDate.getTime() >= expireDateBegin.getTime() && expireDate.getTime() < expireDateEnd.getTime()){
            return true;
        }

        return false;
    }

    public boolean isExpired(){
        Date now = new Date();
        if(now.after(expireDateEnd)){
            return true;
        }else{
            return false;
        }
    }

    public void put(String key, T value){
        this.data.put(key, value);
    }

    public T get(String key){
        return this.data.get(key);
    }

    public void remove(String key){
        this.data.remove(key);
    }

    public boolean containsKey(String key){
        return data.containsKey(key);
    }

}