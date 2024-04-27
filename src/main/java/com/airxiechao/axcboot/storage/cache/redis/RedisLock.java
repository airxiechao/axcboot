package com.airxiechao.axcboot.storage.cache.redis;

import com.airxiechao.axcboot.util.UuidUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.params.SetParams;

public class RedisLock implements AutoCloseable{

    private static final Logger logger = LoggerFactory.getLogger(RedisLock.class);

    private static final String OK = "OK";

    protected Redis redis;
    protected String key;
    protected String value;
    protected Integer numExpireSec;

    public RedisLock(Redis redis, String key, Integer numExpireSec){
        this.redis = redis;
        this.key = key;
        this.value = UuidUtil.random();
        this.numExpireSec = numExpireSec;
    }

    public void lock(){
        while(!set()){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {

            }
        }
    }

    public boolean tryLock(){
        return set();
    }

    public void unlock(){
        redis.execute(jedis -> {
            Object ret = jedis.get(key);
            if(this.value.equals(ret)){
                jedis.del(key);
            }

            return null;
        });
    }

    private boolean set(){
        Object ret = redis.execute(jedis -> {
            SetParams setParams = new SetParams().nx();
            if(null != numExpireSec){
                setParams.ex(numExpireSec);
            }

            return jedis.set(key, value, setParams);
        });

        return OK.equals(ret);
    }

    @Override
    public void close() throws Exception {
        unlock();
    }
}
