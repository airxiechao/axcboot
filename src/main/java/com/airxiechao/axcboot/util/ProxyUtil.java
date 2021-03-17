package com.airxiechao.axcboot.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

public class ProxyUtil {

    public static <T> T buildProxy(Class<T> cls, InvocationHandler h){
        T proxyInstance = (T) Proxy.newProxyInstance(
                ProxyUtil.class.getClassLoader(),
                new Class[] { cls },
                h);
        return proxyInstance;
    }

}
