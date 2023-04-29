package com.airxiechao.axcboot.communication.websocket.common;

import com.alibaba.fastjson.JSON;

public class WsMessageDecoder {
    public static WsMessage decode(String message){
        WsMessage wsMessage = JSON.parseObject(message, WsMessage.class);
        return wsMessage;
    }
}
