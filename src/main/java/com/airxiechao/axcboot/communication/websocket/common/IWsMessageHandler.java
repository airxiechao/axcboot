package com.airxiechao.axcboot.communication.websocket.common;

import com.airxiechao.axcboot.communication.websocket.server.WsServer;
import io.undertow.websockets.core.WebSocketChannel;

public interface IWsMessageHandler {

    Object handle(Object payload, WsServer server, WebSocketChannel channel) throws Exception;
}
