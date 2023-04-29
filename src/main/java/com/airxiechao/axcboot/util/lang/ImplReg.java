package com.airxiechao.axcboot.util.lang;

import com.airxiechao.axcboot.util.ProxyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ImplReg {

    private static final Logger logger = LoggerFactory.getLogger(ImplReg.class);

    protected Map<Class, Object> implMap = new ConcurrentHashMap<>();

    /**
     * 注册接口和实现
     * @param interfaceCls
     * @param implCls
     * @param <T>
     */
    public <T> void registerImpl(Class<T> interfaceCls, Class<? extends T> implCls){
        try {
            Constructor<? extends T> constructor = implCls.getDeclaredConstructor();
            constructor.setAccessible(true);
            T impl = constructor.newInstance();
            registerImpl(interfaceCls, impl);
        } catch (Exception e){
            logger.error("register implementation error", e);
        }
    }

    public <T> void registerImpl(Class<T> interfaceCls, Object impl){
        implMap.put(interfaceCls, impl);
    }

    /**
     * 获取接口的实现
     * @param interfaceCls
     * @param <T>
     * @return
     */
    public <T> T getImpl(Class<T> interfaceCls){
        T impl = (T)implMap.get(interfaceCls);
        if(null == impl){
            logger.warn("no implementation of {}", interfaceCls);
        }

        return impl;
    }

    /**
     * 获取接口的动态代理实现
     * @Param reg
     * @param interfaceCls
     * @param <T>
     * @return
     */
    public static <T> T getImplProxy(ImplReg reg, Class<T> interfaceCls){
        return ProxyUtil.buildProxy(interfaceCls, (proxy, method, args) -> {
            T implObj = reg.getImpl(interfaceCls);
            try{
                return method.invoke(implObj, args);
            }catch (InvocationTargetException e){
                throw e.getCause();
            }

        });
    }

    /**
     * 获取接口的动态代理实现
     * @Param reg
     * @param interfaceCls
     * @param <T>
     * @return
     */
    public static <T> T getImplProxy(Supplier<? extends ImplReg> regSupplier, Class<T> interfaceCls){
        return ProxyUtil.buildProxy(interfaceCls, (proxy, method, args) -> {
            ImplReg reg = regSupplier.get();
            T implObj = reg.getImpl(interfaceCls);
            try{
                return method.invoke(implObj, args);
            }catch (InvocationTargetException e){
                throw e.getCause();
            }
        });
    }

}
