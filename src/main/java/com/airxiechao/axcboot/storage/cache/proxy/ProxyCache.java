package com.airxiechao.axcboot.storage.cache.proxy;

import com.airxiechao.axcboot.storage.cache.expire.ExpiringCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ProxyCache {
    private static final Logger logger = LoggerFactory.getLogger(ProxyCache.class);

    private ExpiringCache expiringCache;

    public ProxyCache(String name, int expirePeriod, ExpiringCache.UNIT unit) {
        this.expiringCache = new ExpiringCache(name, expirePeriod, unit);
    }

    public <T> T get(T object) {
        T proxyInstance = (T) Proxy.newProxyInstance(
                ProxyCache.class.getClassLoader(),
                object.getClass().getInterfaces(),
                (proxy, method, args) -> invoke(object, method, args));

        return proxyInstance;
    }

    private Object invoke(Object object, Method method, Object[] args) {
        String cacheName = buildCacheName(method, args);
        Object value = getCache(cacheName);
        if(null != value){
            logger.info("命中缓存：" + cacheName);
            return value;
        }

        try {
            value = method.invoke(object, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        putCache(cacheName, value);
        return value;
    }

    private void putCache(String cacheName, Object value){
        logger.info("放入缓存：" + cacheName);
        expiringCache.put(cacheName, value);
    }

    private Object getCache(String cacheName){
        return expiringCache.get(cacheName);
    }

    private String buildCacheName(Method method, Object[] args){
        StringBuilder sb = new StringBuilder();
        sb.append(method.getDeclaringClass().getName());
        sb.append(".");
        sb.append(method.getName());
        if(null != args){
            for (Object arg : args) {
                sb.append("|");
                sb.append(arg);
            }
        }
        String cacheName = sb.toString();
        return cacheName;
    }
}
