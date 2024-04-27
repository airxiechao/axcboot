package com.airxiechao.axcboot.storage.cache.redis;

import com.airxiechao.axcboot.storage.db.sql.model.OrderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RedisRangeSet {

    private static final Logger logger = LoggerFactory.getLogger(RedisRangeSet.class);

    protected Redis redis;
    protected String tableName;

    public RedisRangeSet(Redis redis, String tableName){
        this.redis = redis;
        this.tableName = tableName;
    }

    /**
     * 添加
     * @param score
     * @param item
     */
    public void add(long score, String item){
        redis.execute(jedis -> jedis.zadd(tableName, score, item));
    }

    /**
     * 删除
     * @param item
     */
    public void remove(String item) {
        redis.execute(jedis -> jedis.zrem(tableName, item));
    }

    /**
     * 计数
     * @param min
     * @param max
     * @return
     * @throws Exception
     */
    public long count(Long min, Long max) {
        String strMin = null != min ? inclusiveWrap(min) : "-inf";
        String strMax = null != max ? exclusiveWrap(max) : "inf";
        long total = (long)redis.execute(jedis -> jedis.zcount(tableName, strMin, strMax));

        return total;
    }

    /**
     * 搜索
     * @param min
     * @param max
     * @param orderType
     * @param pageNo
     * @param pageSize
     * @return
     */
    public List<String> search(Long min, Long max,
                               String orderType, Integer pageNo, Integer pageSize
    ) {
        // 总数
        long total = count(min, max);

        Set<String> set;
        if(OrderType.DESC.equals(orderType)){
            // 降序
            long start = 0;
            if(null != max){
                start = (long)redis.execute(jedis -> jedis.zcount(tableName, inclusiveWrap(max), "inf"));
            }
            long end = start + total;

            long pageStart = start;
            long pageEnd = end;
            if(null != pageNo && null != pageSize){
                pageStart = start + (pageNo - 1) * pageSize;
                pageEnd = start + pageNo * pageSize;
                if(pageEnd > end){
                    pageEnd = end;
                }
            }

            long ps = pageStart;
            long pe = Math.max(pageEnd - 1, 0);
            set = (Set<String>)redis.execute(jedis -> jedis.zrevrange(tableName, ps, pe));
        }else{
            // 升序
            long start = 0;
            if(null != min) {
                start = (long)redis.execute(jedis -> jedis.zcount(tableName, "-inf", exclusiveWrap(min)));
            }
            long end = start + total;

            long pageStart = start;
            long pageEnd = end;
            if(null != pageNo && null != pageSize){
                pageStart = start + (pageNo - 1) * pageSize;
                pageEnd = start + pageNo * pageSize;
                if(pageEnd > end){
                    pageEnd = end;
                }
            }

            long ps = pageStart;
            long pe = Math.max(pageEnd - 1, 0);
            set = (Set<String>)redis.execute(jedis -> jedis.zrange(tableName, ps, pe));
        }

        return new ArrayList<>(set);
    }

    protected String exclusiveWrap(long score){
        return "("+score;
    }

    protected String inclusiveWrap(long score){
        return ""+score;
    }
}
