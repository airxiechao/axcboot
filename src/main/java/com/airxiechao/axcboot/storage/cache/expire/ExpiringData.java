package com.airxiechao.axcboot.storage.cache.expire;

import java.util.Date;


/**
 * 数据和它的过期时间
 * @param <T>
 */
public class ExpiringData<T> {

    private T data;
    private Date expireDate;

    public ExpiringData(T data, Date expreDate){
        this.data = data;
        this.expireDate = expreDate;
    }

    public T getData(){
        return data;
    }

    public Date getExpireDate(){
        return expireDate;
    }

    public boolean isExpired(){
        Date now = new Date();
        if(now.after(expireDate)){
            return true;
        }else{
            return false;
        }
    }
}
