package com.airxiechao.axcboot.communication.websocket.server;

import com.airxiechao.axcboot.communication.websocket.annotation.WsMessageType;
import com.airxiechao.axcboot.communication.websocket.common.IWsMessageHandler;
import com.airxiechao.axcboot.communication.websocket.common.WsMessage;
import com.airxiechao.axcboot.communication.websocket.common.WsMessageDecoder;
import com.alibaba.fastjson.JSON;
import io.undertow.Undertow;
import io.undertow.websockets.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static io.undertow.Handlers.*;

public class WsServer {

    private static final Logger logger = LoggerFactory.getLogger(WsServer.class);

    private Map<String, IWsMessageHandler> serviceHandlers = new HashMap<>();

    private Undertow server;
    private String ip;
    private String endpoint;
    private int port;

    protected Set<WebSocketChannel> peerConnections = ConcurrentHashMap.newKeySet();

    public WsServer(String ip, int port, String endpoint){
        this.ip = ip;
        this.port = port;
        this.endpoint = endpoint;

        if(!this.endpoint.startsWith("/")){
            this.endpoint = "/" + this.endpoint;
        }
    }

    public void start(){
        WsServerMessageRouter router = new WsServerMessageRouter(serviceHandlers, this);

        server = Undertow.builder()
                .addHttpListener(this.port, this.ip)
                .setHandler(path()
                        .addPrefixPath(this.endpoint, websocket((exchange, channel) -> {
                            // on connect
                            peerConnections.add(channel);

                            channel.getReceiveSetter().set(new AbstractReceiveListener() {

                                @Override
                                protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
                                    final String messageData = message.getData();
                                    WsMessage wsMessage = WsMessageDecoder.decode(messageData);
                                    try {
                                        router.handleServiceMessage(channel, wsMessage);
                                    } catch (Exception e) {
                                        logger.error("websocket message handler error", e);
                                    }
                                }

                                @Override
                                protected void onCloseMessage(CloseMessage cm, WebSocketChannel channel) {
                                    removeChannel(channel);
                                }

                                @Override
                                protected void onError(WebSocketChannel channel, Throwable error) {
                                    super.onError(channel, error);
                                    removeChannel(channel);
                                }

                            });
                            channel.resumeReceives();
                        }))
                ).build();
        server.start();
    }

    public void stop(){
        server.stop();
    }

    public WsServer registerHandler(Class<?> cls){
        Method[] methods = cls.getDeclaredMethods();
        for(Method method : methods){
            method.setAccessible(true);

            WsMessageType type = method.getAnnotation(WsMessageType.class);
            if(null != type){
                String typeStr = type.value();
                IWsMessageHandler handler = (payload, server, channel) -> {
                    return method.invoke(null, payload, server, channel);
                };

                serviceHandlers.put(typeStr, handler);
            }
        }

        return this;
    }

    protected void removeChannel(WebSocketChannel channel){
        peerConnections.remove(channel);
    }

}
