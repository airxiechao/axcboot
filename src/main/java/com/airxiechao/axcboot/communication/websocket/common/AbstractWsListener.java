package com.airxiechao.axcboot.communication.websocket.common;

import com.airxiechao.axcboot.communication.rest.security.AuthPrincipal;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;

public abstract class AbstractWsListener extends AbstractReceiveListener {

    public abstract void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel);
    public abstract void onClose(WebSocketChannel channel);
    public abstract void onError(WebSocketChannel channel);
    public abstract void onMessage(WebSocketChannel channel, String message);

    // on message
    @Override
    protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage data) {
        final String message = data.getData();
        onMessage(channel, message);
    }

    // on close
    @Override
    protected void onCloseMessage(CloseMessage cm, WebSocketChannel channel) {
        onClose(channel);
    }

    // on error
    @Override
    protected void onError(WebSocketChannel channel, Throwable error) {
        super.onError(channel, error);
        onError(channel);
    }
}
