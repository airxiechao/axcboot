package com.airxiechao.axcboot;

import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.rest.annotation.Get;
import com.airxiechao.axcboot.communication.common.annotation.Param;
import com.airxiechao.axcboot.communication.rest.server.RestServer;
import com.airxiechao.axcboot.communication.rest.util.RestUtil;
import com.airxiechao.axcboot.crypto.SslUtil;
import com.airxiechao.axcboot.storage.fs.LocalFs;
import com.airxiechao.axcboot.util.*;
import com.airxiechao.axcboot.util.HttpUtil;
import com.alibaba.fastjson.JSON;
import io.undertow.server.HttpServerExchange;

import java.util.Map;

public class RestTestServer {

    public static void main(String[] args) throws Exception {

        RestServer restServer = new RestServer("test");
        restServer.config("0.0.0.0", 443, null,
                SslUtil.buildKeyManagerFactory(new LocalFs("d:/test"), "rest-server-key.jks", "123456"),
                null,
            (token, scope, item, mode) -> {
                return false;
            });

        restServer
                .registerConsul(10, "")
                .registerHandler(RestHandler.class)
                .registerStatic("/", "html",
                        "index.html", "login.html");

        restServer.start();

        Thread.sleep(1000);

        Map params = new MapBuilder<String, String>()
                .put("a", "1")
                .put("b", "2")
                .build();
        Response resp = RestClient.getBySsl(Add.class).add(params);
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

            String ret = HttpUtil.get("http://127.0.0.1/api"+path, params, null, null, 10, false);
            return JSON.parseObject(ret, Response.class);
        });
    }

    public static <T> T getBySsl(Class<T> cls){
        return ProxyUtil.buildProxy(cls, (proxy, method, args) -> {
            String path = AnnotationUtil.getMethodAnnotation(method, Get.class).value();
            Map params = (Map)args[0];

            String ret = HttpUtil.get("https://127.0.0.1:443/api"+path, params, null, null, 10, true);
            return JSON.parseObject(ret, Response.class);
        });
    }
}

