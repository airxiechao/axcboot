package com.airxiechao.axcboot.storage.cache.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

public class Redis {

    private static final int TIMEOUT = 5000;

    private String name;
    private JedisPool pool;

    public Redis(String name, String ip, int port, String password, int maxPoolSize){

        this.name = name;

        JedisPoolConfig poolConfig = buildPoolConfig(maxPoolSize);

        pool = new JedisPool(poolConfig, ip, port, TIMEOUT, password);
    }

    /**
     * 配置连接池
     * @return
     */
    private JedisPoolConfig buildPoolConfig(int maxPoolSize){
        JedisPoolConfig poolConfig = new JedisPoolConfig();

        poolConfig.setMaxTotal(maxPoolSize);
        poolConfig.setMaxIdle(maxPoolSize);
        poolConfig.setMinIdle(0);

        poolConfig.setBlockWhenExhausted(true);
        poolConfig.setMaxWaitMillis(TIMEOUT);

        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
        poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
        poolConfig.setNumTestsPerEvictionRun(3);

        return poolConfig;
    }

    public Object execute(IRedisRunnable runnable) {
        try (Jedis jedis = pool.getResource()) {
            return runnable.run(jedis);
        }
    }

    public void close(){
        pool.close();
    }

    public interface IRedisRunnable{
        Object run(Jedis jedis);
    }
}
