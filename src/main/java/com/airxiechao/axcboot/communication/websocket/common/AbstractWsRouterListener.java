package com.airxiechao.axcboot.communication.websocket.common;

import com.airxiechao.axcboot.communication.rest.util.RestUtil;
import com.airxiechao.axcboot.communication.websocket.annotation.WsHandler;
import com.alibaba.fastjson.JSON;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractWsRouterListener extends AbstractWsListener{

    private static final Logger logger = LoggerFactory.getLogger(AbstractWsRouterListener.class);

    private WsMessageRouter router = new WsMessageRouter();

    @Override
    public void onMessage(WebSocketChannel channel, String message) {
        WsMessage wsMessage = WsMessageDecoder.decode(message);
        try {
            router.handleServiceMessage(channel, wsMessage);
        } catch (Exception e) {
            logger.error("ws message handler error", e);
        }
    }

    public AbstractWsRouterListener registerHandler(Class<?> cls){
        router.registerHandler(cls);
        return this;
    }

    public class WsMessageRouter {

        private Map<String, IWsMessageHandler> handlers = new HashMap<>();

        public void registerHandler(Class<?> cls){
            Method[] methods = cls.getDeclaredMethods();
            for(Method method : methods){
                method.setAccessible(true);

                WsHandler type = method.getAnnotation(WsHandler.class);
                if(null != type){
                    this.handlers.put(type.value(), (payload, channel) -> {
                        return method.invoke(null, payload, channel);
                    });
                }
            }
        }

        public void handleServiceMessage(WebSocketChannel channel, WsMessage wsMessage) throws Exception {
            IWsMessageHandler handler = handlers.get(wsMessage.getType());
            if(null != handler){
                Object response = handler.handle(wsMessage.getPayload(), channel);

                WsMessage wsMessageResponse = new WsMessage(wsMessage.getRequestId(), wsMessage.getType()+"_response", response);

                RestUtil.sendWsObject(wsMessageResponse, channel, null);
            }
        }
    }
}
