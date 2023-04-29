package com.airxiechao.axcboot.communication.websocket.server;

import io.undertow.websockets.core.WebSocketChannel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WsSessionFactory<T> {
    protected Map<WebSocketChannel, T> sessionMap = new ConcurrentHashMap<>();

    public T createSession(WebSocketChannel channel, T session){
        sessionMap.put(channel, session);
        return session;
    }

    public T getSession(WebSocketChannel channel){
        return sessionMap.get(channel);
    }

    public void destroySession(WebSocketChannel channel){
        sessionMap.remove(channel);
    }
}
