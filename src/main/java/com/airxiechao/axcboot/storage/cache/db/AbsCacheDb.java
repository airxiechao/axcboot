package com.airxiechao.axcboot.storage.cache.db;

import com.airxiechao.axcboot.storage.db.sql.DbManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbsCacheDb<K, T> {

    private static final Logger logger = LoggerFactory.getLogger(AbsCacheDb.class);

    protected DbManager dbManager;
    protected Class<T> cls;
    protected Map<K, Optional<T>> cache = new ConcurrentHashMap<>();

    public AbsCacheDb(DbManager dbManager, Class<T> cls){
        this.dbManager = dbManager;
        this.cls = cls;
    }

    /**
     * 获取
     * @param key
     * @return
     */
    public T get(K key){
        if(cache.containsKey(key)){
            Optional<T> opt = cache.get(key);
            if(opt.isPresent()){
                return opt.get();
            }else{
                return null;
            }
        }else{
            T obj = getDbByKey(key);
            cache.put(key, Optional.ofNullable(obj));

            return obj;
        }
    }

    /**
     * 添加
     * @param obj
     * @return
     */
    public boolean insert(T obj){
        boolean inserted = dbManager.insert(obj) > 0;
        if(inserted){
            cache.put(buildKey(obj), Optional.of(obj));
        }

        return inserted;
    }

    /**
     * 修改
     * @param obj
     * @return
     */
    public boolean update(T obj){
        boolean updated = dbManager.update(obj) > 0;
        if(updated){
            cache.put(buildKey(obj), Optional.of(obj));
        }

        return updated;
    }

    /**
     * 删除
     * @param key
     * @return
     */
    public boolean delete(K key){
        cache.remove(key);
        return deleteDbByKey(key);
    }

    protected abstract T getDbByKey(K key);

    protected abstract boolean deleteDbByKey(K key);

    protected abstract K buildKey(T obj);

}
