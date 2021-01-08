package com.airxiechao.axcboot;

import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.websocket.annotation.WsMessageType;
import com.airxiechao.axcboot.communication.websocket.server.WsServer;
import com.alibaba.fastjson.JSON;
import io.undertow.websockets.core.WebSocketChannel;

import java.util.Map;

public class WsServerTest {

    public static void main(String args[]){
        WsServer wsServer = new WsServer("0.0.0.0", 80, "/ws");
        wsServer.registerHandler(ParkWsHandler.class);
        wsServer.start();
    }
}

class ParkWsHandler {

    @WsMessageType("add")
    public static Object add(Object payload, WsServer server, WebSocketChannel channel){
        Map map = JSON.parseObject((String)payload, Map.class);

        Response resp = new Response();
        resp.success();
        resp.setData((Integer)map.get("a")+(Integer)map.get("b"));

        return resp;
    }
}
