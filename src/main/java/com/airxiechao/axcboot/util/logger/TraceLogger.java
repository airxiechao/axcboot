package com.airxiechao.axcboot.util.logger;

import org.slf4j.Logger;

import java.lang.reflect.Proxy;
import java.util.Random;

public class TraceLogger {

    public static Logger wrap(Logger logger){

        // calculate trace id

        // current millisecond
        String ms = (System.currentTimeMillis() % 1000) +"";
        for(int i = 0; i < 3 - ms.length(); ++i){
            ms = "0" + ms;
        }

        // random int
        ms += new Random().nextInt(10);

        TraceLoggerInvocationHandler invocationHandler = new TraceLoggerInvocationHandler(logger, ms);

        Logger proxyObject = (Logger) Proxy.newProxyInstance(TraceLogger.class.getClassLoader(),
                logger.getClass().getInterfaces(), invocationHandler);

        return proxyObject;
    }

}
