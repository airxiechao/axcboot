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
import com.alibaba.fastjson.JSONObject;
import io.undertow.server.HttpServerExchange;

import java.util.Map;

public class RestTestServer {

    public static void main(String[] args) throws Exception {

        RestServer restServer = new RestServer("test");
        restServer.config("0.0.0.0", 88, null,
                null, //SslUtil.buildKeyManagerFactory(new LocalFs("d:/test"), "rest-server-key.jks", "123456"),
                null,
            (token, scope, item, mode) -> {
                return false;
            }, (exchange, method) -> {
                    String jsonString = JSON.toJSONString(RestUtil.allQueryStringParam(exchange));
                    return JSON.parseObject(jsonString, method.getParameterTypes()[0]);
            });

        restServer
                .registerConsul("localhost", 8500, 10, "")
                .registerHandler(RestHandler.class)
                .registerStatic("/", "html",
                        "index.html", "login.html");

        restServer.start();

        Thread.sleep(1000);

        ABParam param = new ABParam(1,2);
        Response resp = RestClient.get(Add.class).add(param);
        System.out.println(resp.getData());
    }

}

class ABParam {
    private Integer a;
    private Integer b;

    public ABParam() {
    }

    public ABParam(Integer a, Integer b) {
        this.a = a;
        this.b = b;
    }

    public Integer getA() {
        return a;
    }

    public void setA(Integer a) {
        this.a = a;
    }

    public Integer getB() {
        return b;
    }

    public void setB(Integer b) {
        this.b = b;
    }
}

interface Add {
    /**
     * 加法：/api/add?a=1&b=2
     * @param param
     * @return
     */
    @Get("/add")
    @Param(value = "a", required = true)
    @Param(value = "b", required = true)
    Response add(ABParam param);
}

class RestHandler implements Add{

//    private HttpServerExchange exchange;
//
//    public RestHandler(HttpServerExchange exchange) {
//        this.exchange = exchange;
//    }

    @Override
    public Response add(ABParam param) {
        Integer a = param.getA();
        Integer b = param.getB();

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
            Map params = ModelUtil.toMap(args[0]);

            String ret = HttpUtil.get("http://127.0.0.1:88/api"+path, params, null, null, 10, false);
            return JSON.parseObject(ret, Response.class);
        });
    }

    public static <T> T getBySsl(Class<T> cls){
        return ProxyUtil.buildProxy(cls, (proxy, method, args) -> {
            String path = AnnotationUtil.getMethodAnnotation(method, Get.class).value();
            Map params = ModelUtil.toMap(args[0]);

            String ret = HttpUtil.get("https://127.0.0.1:443/api"+path, params, null, null, 10, true);
            return JSON.parseObject(ret, Response.class);
        });
    }
}

