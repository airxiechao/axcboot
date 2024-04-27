package com.airxiechao.axcboot.storage.cache.redis;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * redis对象缓存表
 * @param <T>
 */
public abstract class AbsRedisTable<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbsRedisTable.class);

    protected Redis redis;
    protected Class<T> cls;
    protected String tableName;
    protected Map<String, Field> rangeIndexFields = new HashMap<>();
    protected Map<String, Field> hashIndexFields = new HashMap<>();

    public AbsRedisTable(Redis redis, String tableName, Class<T> cls,
                         String[] rangeIndexFieldNames,
                         String[] hashIndexFieldNames){
        this.redis = redis;
        this.cls = cls;

        this.tableName = tableName;

        if(null != rangeIndexFieldNames){
            Arrays.stream(rangeIndexFieldNames).forEach(fieldName -> {
                try {
                    Field field = cls.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    rangeIndexFields.put(fieldName, field);
                } catch (NoSuchFieldException e) {
                    logger.error("redis table cache [{}] no field [{}] for range index", tableName, fieldName);
                }
            });
        }

        if(null != hashIndexFieldNames){
            Arrays.stream(hashIndexFieldNames).forEach(fieldName -> {
                try {
                    Field field = cls.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    hashIndexFields.put(fieldName, field);
                } catch (NoSuchFieldException e) {
                    logger.error("redis table cache [{}] no field [{}] for hash index", tableName, fieldName);
                }
            });
        }
    }

    protected String buildIndexName(String field){
        return tableName+":"+field;
    }

    public abstract String buildObjectKey(T object);

    public abstract Long buildRangeIndexScore(String fieldName, Object value);

    public abstract String buildHashIndexKey(String fieldName, Object value);


    /**
     * 获取
     * @param objectKey
     * @return
     */
    public T get(String objectKey){
        T object = JSON.parseObject((String)redis.execute(jedis -> jedis.hget(tableName, objectKey)), cls);
        return object;
    }

    /**
     * 添加
     * @param object
     */
    public void set(T object){
        String objectKey = buildObjectKey(object);

        // add object
        redis.execute(jedis -> jedis.hset(tableName, objectKey, JSON.toJSONString(object)));

        // add range index
        rangeIndexFields.forEach((fieldName, field) -> {
            String indexName = buildIndexName(fieldName);
            try {
                long indexScore = buildRangeIndexScore(fieldName, field.get(object));
                new RedisRangeSet(this.redis, indexName).add(indexScore, objectKey);
            } catch (Exception e) {
                logger.error("redis table cache [{}] set range index [{}] error", tableName, indexName, e);
            }
        });

        // add hash index
        hashIndexFields.forEach((fieldName, field) -> {
            String indexName = buildIndexName(fieldName);
            try {
                String indexKey = buildHashIndexKey(fieldName, field.get(object));
                new RedisListTable(this.redis, indexName).push(indexKey, objectKey);
            } catch (Exception e) {
                logger.error("redis table cache [{}] set hash index [{}] error", tableName, indexName, e);
            }
        });
    }

    /**
     * 删除
     * @param objectKey
     */
    public void remove(String objectKey){
        T oldObject = get(objectKey);

        // remove object
        redis.execute(jedis -> jedis.hdel(tableName, objectKey));

        // remove range index
        rangeIndexFields.forEach((fieldName, field) -> {
            String indexName = buildIndexName(fieldName);
            new RedisRangeSet(this.redis, indexName).remove(objectKey);
        });

        // remove hash index
        hashIndexFields.forEach((fieldName, field) -> {
            String indexName = buildIndexName(fieldName);
            try{
                String indexKey = buildHashIndexKey(fieldName, field.get(oldObject));
                new RedisListTable(this.redis, indexName).remove(indexKey, objectKey);
            }catch (Exception e) {
                logger.error("redis table cache [{}] remove hash index [{}] error", tableName, indexName, e);
            }

        });
    }

    /**
     * range索引计数
     * @param fieldName
     * @param min
     * @param max
     * @return
     * @throws Exception
     */
    public long rangeCount(String fieldName, Object min, Object max) {
        if(!rangeIndexFields.containsKey(fieldName)){
            logger.warn("no range index field [{}]", fieldName);
            return 0;
        }

        // 总数
        String indexName = buildIndexName(fieldName);
        return new RedisRangeSet(this.redis, indexName).count(buildRangeIndexScore(fieldName, min), buildRangeIndexScore(fieldName, max));
    }

    /**
     * range索引搜索
     * @param fieldName
     * @param min
     * @param max
     * @param orderType
     * @param pageNo
     * @param pageSize
     * @return
     */
    public List<T> rangeSearch(String fieldName, Object min, Object max,
                                    String orderType, Integer pageNo, Integer pageSize
    ) {
        if(!rangeIndexFields.containsKey(fieldName)){
            logger.warn("no range index field [{}]", fieldName);
            return new ArrayList<>();
        }

        // 总数
        String indexName = buildIndexName(fieldName);
        List<String> objectKeys = new RedisRangeSet(this.redis, indexName)
                .search(buildRangeIndexScore(fieldName, min), buildRangeIndexScore(fieldName, max), orderType, pageNo, pageSize);

        List<T> list = new ArrayList<>();
        objectKeys.forEach(key -> {
            T object = JSON.parseObject((String)redis.execute(jedis -> jedis.hget(tableName, key)), cls);
            list.add(object);
        });

        return list;
    }

    /**
     * hash索引计数
     * @param fieldName
     * @param value
     * @return
     * @throws Exception
     */
    public long hashCount(String fieldName, Object value) {
        if(!hashIndexFields.containsKey(fieldName)){
            logger.warn("no hash index field [{}]", fieldName);
            return 0;
        }

        String indexName = buildIndexName(fieldName);
        String indexKey = buildHashIndexKey(fieldName, value);

        return new RedisListTable(this.redis, indexName).count(indexKey);
    }

    /**
     * hash索引搜索
     * @param fieldName
     * @param value
     * @param pageNo
     * @param pageSize
     * @return
     */
    public List<T> hashSearch(String fieldName, Object value, Integer pageNo, Integer pageSize) {
        if (!hashIndexFields.containsKey(fieldName)) {
            logger.warn("no hash index field [{}]", fieldName);
            return new ArrayList<>();
        }

        String indexName = buildIndexName(fieldName);
        String indexKey = buildHashIndexKey(fieldName, value);
        List<String> objectKeys = new RedisListTable(this.redis, indexName).search(indexKey, pageNo, pageSize);

        return objectKeys.stream().map(objectKey -> get(objectKey)).collect(Collectors.toList());
    }

}
