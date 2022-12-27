package com.airxiechao.axcboot.communication.rest.resolver;

import io.undertow.server.HttpServerExchange;

import java.lang.reflect.Method;

public interface IParamResolver {
    Object resolve(HttpServerExchange exchange, Method method) throws Exception;
}
