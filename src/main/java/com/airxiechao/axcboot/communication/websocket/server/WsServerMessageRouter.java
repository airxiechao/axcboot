package com.airxiechao.axcboot.communication.websocket.server;

import com.airxiechao.axcboot.communication.websocket.common.IWsMessageHandler;
import com.airxiechao.axcboot.communication.websocket.common.WsMessage;
import com.alibaba.fastjson.JSON;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

import java.util.Map;

public class WsServerMessageRouter {

    private Map<String, IWsMessageHandler> handlers;
    private WsServer server;

    public WsServerMessageRouter(Map<String, IWsMessageHandler> handlers, WsServer server){
        this.handlers = handlers;
        this.server = server;
    }

    public void handleServiceMessage(WebSocketChannel channel, WsMessage wsMessage) throws Exception {
        IWsMessageHandler handler = handlers.get(wsMessage.getType());
        if(null != handler){
            Object response = handler.handle(wsMessage.getPayload(), server, channel);

            WsMessage wsMessageResponse = new WsMessage(wsMessage.getRequestId(), wsMessage.getType()+"_response", response);
            String message = JSON.toJSONString(wsMessageResponse);

            WebSockets.sendText(message, channel, null);
        }
    }
}
