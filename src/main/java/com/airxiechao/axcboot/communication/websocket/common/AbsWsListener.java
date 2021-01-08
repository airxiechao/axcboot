package com.airxiechao.axcboot.communication.websocket.common;

import com.airxiechao.axcboot.communication.rest.security.AuthPrincipal;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;


public abstract class AbsWsListener extends AbstractReceiveListener {

    public abstract void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) throws Exception;

    public abstract boolean hasRole(WebSocketHttpExchange exchange, AuthPrincipal principal, String[] roles);

}
