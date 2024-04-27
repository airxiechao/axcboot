package com.airxiechao.axcboot.storage.cache.expire;

import java.util.Calendar;
import java.util.Date;


/**
 * 数据和它的过期时间
 * @param <T>
 */
public class ExpiringData<T> {

    private T data;
    private Date expireTime;

    public ExpiringData(T data, Date expireTime){
        this.data = data;
        this.expireTime = expireTime;
    }

    public ExpiringData(T data, int expireSeconds){
        this.data = data;
        this.expireTime = buildExpireTime(expireSeconds);
    }

    public T getData(){
        return data;
    }

    public Date getExpireTime(){
        return expireTime;
    }

    public boolean isExpired(){
        Date now = new Date();
        if(now.after(expireTime)){
            return true;
        }else{
            return false;
        }
    }

    private Date buildExpireTime(int expireSeconds){
        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.SECOND, expireSeconds);

        return calendar.getTime();
    }
}
