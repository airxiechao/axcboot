package com.airxiechao.axcboot.communication.rest.healthcheck;

import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.common.annotation.Auth;
import com.airxiechao.axcboot.communication.rest.annotation.Get;
import io.undertow.server.HttpServerExchange;

import java.lang.reflect.Method;

public class HealthCheckRestHandler {

    public static Method getHealthCheckMethod(){
        try {
            return HealthCheckRestHandler.class.getMethod("healthCheck", HttpServerExchange.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("get health check method error", e);
        }
    }

    @Get("/health/check")
    @Auth(ignore = true)
    public static Response healthCheck(HttpServerExchange exchange){
        return new Response();
    }
}
