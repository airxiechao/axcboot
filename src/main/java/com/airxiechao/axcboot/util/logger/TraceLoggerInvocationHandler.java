package com.airxiechao.axcboot.util.logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class TraceLoggerInvocationHandler implements InvocationHandler {

    private Object target;
    private String traceId;

    public TraceLoggerInvocationHandler(Object target, String traceId) {
        this.target = target;
        this.traceId = traceId;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        args[0] = "/" + this.traceId + "/ " + args[0];
        Object returnvalue = method.invoke(target, args);
        return returnvalue;
    }

}