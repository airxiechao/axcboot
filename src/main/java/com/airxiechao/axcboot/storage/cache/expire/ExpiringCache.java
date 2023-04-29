package com.airxiechao.axcboot.storage.cache.expire;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 过期缓存
 *
 * 假设读多写少
 * 数据放在过期时间范围和有效期长度一样的时间桶里
 * 时间桶包含过期时间在它的过期时间范围的数据
 */
public class ExpiringCache<T> {

    private static final Logger logger = LoggerFactory.getLogger(ExpiringCache.class);

    public enum UNIT {
        SECOND,
        MINUTE,
        HOUR,
        DAY,
    }

    /**
     * 读写锁
     */
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock r = rwl.readLock();
    private final Lock w = rwl.writeLock();

    /**
     * 缓存仓库名称
     */
    private String cacheName;

    /**
     * 过期的单位时间数目
     */
    private int expirePeriod;

    /**
     * 时间单位
     */
    private UNIT unit;

    /**
     * 缓存存储
     */
    private List<ExpiringBucket<ExpiringData<T>>> buckets;

    /**
     * 构造函数
     * @param name
     * @param expirePeriod
     * @param unit
     */
    public ExpiringCache(String name, int expirePeriod, UNIT unit){
        this.cacheName = name;
        this.expirePeriod = expirePeriod;
        this.unit = unit;
        this.buckets = new LinkedList<>();
    }

    /**
     * 先清理过期，再添加到缓存，如果已存在则更新
     * @param key
     * @param value
     */
    public void put(String key, T value){
        w.lock();
        try {
            _clearExpiredBucket();
            _put(key, value);

        } finally {
            w.unlock();
        }
    }

    /**
     * 先清理过期，再删除键
     */
    public void remove(String key){
        w.lock();
        try {
            _clearExpiredBucket();
            _remove(key);
        } finally {
            w.unlock();
        }
    }

    /**
     * 从缓存获取
     * @param key
     * @return
     */
    public T get(String key){
        r.lock();
        try {
            return _get(key);
        } finally {
            r.unlock();
        }

    }

    /**
     * 检查是否存在键
     */
    public boolean containsKey(String key){
        r.lock();
        try {
            return _containsNotExpiredKey(key);
        } finally {
            r.unlock();
        }
    }


    // ------------------------------- 私有方法 -------------------------------


    /**
     * 清理过期缓存
     */
    private void _clearExpiredBucket(){
        Iterator<ExpiringBucket<ExpiringData<T>>> iter = buckets.iterator();
        while(iter.hasNext()){
            ExpiringBucket bucket = iter.next();
            if(bucket.isExpired()){
                iter.remove();
            }else{
                break;
            }
        }
    }

    /**
     * 添加
     * @param key
     */
    private void _put(String key, T value){
        /**
         * first find if there is a bucket containing key
         * if exists, remove key from that bucket
         */
        _remove(key);

        /**
         * find if there is a bucket containing current data expire date
         * if not exist, create a bucket with bucket expire date range
         */
        Date now = new Date();
        Date expireDate = _getCurrentDataExpiredDate(now);
        ExpiringBucket<ExpiringData<T>> bucket = _getBucketByExpireDate(expireDate);
        if(null == bucket){
            // if no bucket
            Date bucketExpreDate = _getCurrentBucketExpiredDateEnd(now);
            bucket = new ExpiringBucket<>(expireDate, bucketExpreDate);
            buckets.add(bucket);
        }

        bucket.put(key, new ExpiringData<>(value, expireDate));
    }


    /**
     * 获取
     * @param key
     */
    private T _get(String key){
        ExpiringBucket<ExpiringData<T>> bucket = _getNotExpiredBucketByKey(key);
        if(null != bucket){
            ExpiringData<T> data =  bucket.get(key);
            if(null != data && !data.isExpired()){
                return data.getData();
            }
        }

        return null;
    }

    /**
     * 删除
     * @param key
     */
    private void _remove(String key){
        ExpiringBucket<ExpiringData<T>> bucket = _getNotExpiredBucketByKey(key);
        if(null != bucket){
            bucket.remove(key);
        }
    }

    /**
     * 是否存在未过期的数据
     * @param key
     * @return
     */
    private boolean _containsNotExpiredKey(String key){
        Iterator<ExpiringBucket<ExpiringData<T>>> iter = buckets.iterator();
        while(iter.hasNext()){
            ExpiringBucket<ExpiringData<T>> bucket = iter.next();
            if(!bucket.isExpired() && bucket.containsKey(key)){
                ExpiringData<T> data = bucket.get(key);
                if(null != data && !data.isExpired()){
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 获取包含某个过期时间的数据桶
     * @param expireDate
     * @return
     */
    private ExpiringBucket<ExpiringData<T>> _getBucketByExpireDate(Date expireDate){

        Iterator<ExpiringBucket<ExpiringData<T>>> iter = buckets.iterator();
        while(iter.hasNext()){
            ExpiringBucket bucket = iter.next();
            if(bucket.containsExpireDate(expireDate)){
                return bucket;
            }
        }

        return null;
    }

    /**
     * 获取包含某个key的未过期数据桶
     * @param key
     * @return
     */
    private ExpiringBucket _getNotExpiredBucketByKey(String key){
        Iterator<ExpiringBucket<ExpiringData<T>>> iter = buckets.iterator();
        while(iter.hasNext()){
            ExpiringBucket bucket = iter.next();

            if(!bucket.isExpired() && bucket.containsKey(key)){
                return bucket;
            }
        }

        return null;
    }

    /**
     * 得到当前时间的数据的过期时间
     * @param now
     * @return
     */
    private Date _getCurrentDataExpiredDate(Date now){
        if(null == now){
            now = new Date();
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(now);

        /**
         * 数据过期时间为 数据插入时间+周期数*周期时间
         */
        cal.add(_convertToCalendarUnit(unit), expirePeriod);
        Date expireDate = cal.getTime();

        return expireDate;
    }

    /**
     * 得到当前时间的bucket的过期时间
     * @param now
     * @return
     */
    private Date _getCurrentBucketExpiredDateEnd(Date now){
        if(null == now){
            now = new Date();
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(now);

        /**
         * 如果数据过期时间不在一个桶内，第一次创建包含这个过期时间的桶的过期结束为 数据过期时间+周期数*周期时间
         */
        cal.add(_convertToCalendarUnit(unit), 2 * expirePeriod);
        Date expireDate = cal.getTime();

        return expireDate;
    }

    /**
     * 时间单位转换为Calendar单位
     * @param unit
     * @return
     */
    private int _convertToCalendarUnit(UNIT unit){
        switch (unit){
            case SECOND:
                return Calendar.SECOND;
            case MINUTE:
                return Calendar.MINUTE;
            case HOUR:
                return Calendar.HOUR;
            case DAY:
                return Calendar.DAY_OF_YEAR;
            default:
                return Calendar.SECOND;
        }
    }

}


