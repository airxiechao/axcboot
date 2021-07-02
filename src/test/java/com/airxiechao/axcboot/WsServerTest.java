package com.airxiechao.axcboot;

import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.rest.security.AuthPrincipal;
import com.airxiechao.axcboot.communication.websocket.annotation.WsHandler;
import com.airxiechao.axcboot.communication.websocket.common.AbstractWsRouterListener;
import com.airxiechao.axcboot.communication.websocket.server.WsServer;
import com.alibaba.fastjson.JSON;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import java.util.Map;

public class WsServerTest {

    public static void main(String args[]){
        WsServer wsServer = new WsServer("ws");
        wsServer.config("0.0.0.0", 80, "/ws", new AbstractWsRouterListener() {
            @Override
            public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {

            }

            @Override
            public void onClose(WebSocketChannel channel) {

            }

            @Override
            public void onError(WebSocketChannel channel) {

            }
        }.registerHandler(ParkWsHandler.class), ((authToken, scope, item, mode) -> true));

        wsServer.start();
    }
}

class ParkWsHandler {

    @WsHandler("add")
    public static Object add(Object payload, WsServer server, WebSocketChannel channel){
        Map map = JSON.parseObject((String)payload, Map.class);

        Response resp = new Response();
        resp.success();
        resp.setData((Integer)map.get("a")+(Integer)map.get("b"));

        return resp;
    }
}
