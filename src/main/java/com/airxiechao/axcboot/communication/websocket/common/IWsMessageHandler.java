package com.airxiechao.axcboot.communication.websocket.common;

import io.undertow.websockets.core.WebSocketChannel;

public interface IWsMessageHandler {

    Object handle(Object payload, WebSocketChannel channel) throws Exception;
}
