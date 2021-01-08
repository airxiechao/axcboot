package com.airxiechao.axcboot;

import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.rest.annotation.Get;
import com.airxiechao.axcboot.communication.common.annotation.Param;
import com.airxiechao.axcboot.communication.rest.security.AuthPrincipal;
import com.airxiechao.axcboot.communication.rest.server.RestServer;
import com.airxiechao.axcboot.communication.rest.util.RestUtil;
import io.undertow.server.HttpServerExchange;

public class RestTestServer {

    public static void main(String[] args){
        RestServer restServer = new RestServer("0.0.0.0", 80, null, null,
            (exchange, principal, roles) -> {
                return false;
            });

        restServer.registerHandler(RestHandler.class)
                .registerStatic("/", "html",
                        "index.html", "login.html", null);

        restServer.start();
    }

}

class RestHandler {

    /**
     * 加法：/rest/add?a=1&b=2
     * @param exchange
     * @return
     */
    @Get("/add")
    @Param(value = "a", required = true)
    @Param(value = "b", required = true)
    public static Response hello(HttpServerExchange exchange) {

        Integer a = RestUtil.queryIntegerParam(exchange, "a");
        Integer b = RestUtil.queryIntegerParam(exchange, "b");

        Response resp = new Response();
        resp.success();
        resp.setData(a+b);

        return resp;
    }
}

