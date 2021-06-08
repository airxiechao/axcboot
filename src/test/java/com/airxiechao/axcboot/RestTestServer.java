package com.airxiechao.axcboot;

import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.rest.annotation.Get;
import com.airxiechao.axcboot.communication.common.annotation.Param;
import com.airxiechao.axcboot.communication.rest.server.RestServer;
import com.airxiechao.axcboot.communication.rest.util.RestUtil;
import com.airxiechao.axcboot.util.AnnotationUtil;
import com.airxiechao.axcboot.util.HttpUtil;
import com.airxiechao.axcboot.util.MapBuilder;
import com.airxiechao.axcboot.util.ProxyUtil;
import com.alibaba.fastjson.JSON;
import io.undertow.server.HttpServerExchange;

import java.util.Map;

public class RestTestServer {

    public static void main(String[] args) throws InterruptedException {
        RestServer restServer = new RestServer("test");
        restServer.config("0.0.0.0", 80, null, null,
            (exchange, principal, roles) -> {
                return false;
            });

        restServer
                .registerConsul(10)
                .registerHandler(RestHandler.class)
                .registerStatic("/", "html",
                        "index.html", "login.html", null);

        restServer.start();

        Thread.sleep(1000);

        Map params = new MapBuilder<String, String>()
                .put("a", "1")
                .put("b", "2")
                .build();
        Response resp = RestClient.get(Add.class).add(params);
        System.out.println(resp.getData());
    }

}

interface Add {
    /**
     * 加法：/api/add?a=1&b=2
     * @param exc
     * @return
     */
    @Get("/add")
    @Param(value = "a", required = true)
    @Param(value = "b", required = true)
    Response add(Object exc);
}

class RestHandler implements Add{

    @Override
    public Response add(Object exc) {

        HttpServerExchange exchange = (HttpServerExchange)exc;

        Integer a = RestUtil.queryIntegerParam(exchange, "a");
        Integer b = RestUtil.queryIntegerParam(exchange, "b");

        Response resp = new Response();
        resp.success();
        resp.setData(a+b);

        return resp;
    }
}

class RestClient {
    public static <T> T get(Class<T> cls){
        return ProxyUtil.buildProxy(cls, (proxy, method, args) -> {
            String path = AnnotationUtil.getMethodAnnotation(method, Get.class).value();
            Map params = (Map)args[0];

            String ret = HttpUtil.get("http://127.0.0.1/api"+path, params, 10);
            return JSON.parseObject(ret, Response.class);
        });
    }
}

