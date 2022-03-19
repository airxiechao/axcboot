package com.airxiechao.axcboot.storage.cache.redis;

import com.airxiechao.axcboot.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RedisListTable {

    private static final Logger logger = LoggerFactory.getLogger(RedisListTable.class);

    private static final String LIST_DELIMITER = ",";

    protected Redis redis;
    protected String tableName;

    public RedisListTable(Redis redis, String tableName){
        this.redis = redis;
        this.tableName = tableName;
    }

    /**
     * 获取
     * @param listKey
     * @param pageNo
     * @param pageSize
     * @return
     */
    public List<String> search(String listKey, Integer pageNo, Integer pageSize){
        String value = (String)redis.execute(jedis -> jedis.hget(tableName, listKey));
        if(StringUtil.isBlank(value)){
            return new ArrayList<>();
        }else{
            List<String> list =  Arrays.asList(value.split(LIST_DELIMITER));
            if(null != pageNo && null != pageSize){
                list = list.stream().skip((pageNo-1)*pageSize).limit(pageSize).collect(Collectors.toList());
            }
            return list;
        }
    }

    /**
     * 计数
     * @param listKey
     * @return
     * @throws Exception
     */
    public long count(String listKey) {
        String value = (String)redis.execute(jedis -> jedis.hget(tableName, listKey));
        if(StringUtil.isBlank(value)){
            return 0;
        }else{
            return value.split(LIST_DELIMITER).length;
        }
    }

    /**
     * 追加
     * @param listKey
     * @param item
     */
    public void push(String listKey, String item){
        redis.execute(jedis -> {
            String oldValue = jedis.hget(tableName, listKey);
            return jedis.hset(tableName, listKey, StringUtil.concat(LIST_DELIMITER, oldValue, item));
        });
    }

    /**
     * 删除
     * @param listKey
     * @param item
     */
    public void remove(String listKey, String item){
        redis.execute(jedis -> {
            String oldValue = jedis.hget(tableName, listKey);

            if(!StringUtil.isBlank(oldValue)){
                String[] oldValues = oldValue.split(LIST_DELIMITER);
                List<String> newValues = Arrays.stream(oldValues)
                        .filter(value -> !value.equals(item))
                        .collect(Collectors.toList());

                if(newValues.size() > 0){
                    return jedis.hset(tableName, listKey, String.join(LIST_DELIMITER, newValues));
                }
            }

            return jedis.hdel(tableName, listKey);
        });
    }

}
