package com.airxiechao.axcboot.communication.websocket.common;

import com.airxiechao.axcboot.communication.common.RequestId;

public class WsMessage {
    private String requestId;
    private String type;
    private Object payload;

    public WsMessage(String requestId, String type, Object payload){
        this.requestId = requestId;
        this.type = type;
        this.payload = payload;
    }

    public WsMessage(String type, Object payload){
        this.requestId = RequestId.next();
        this.type = type;
        this.payload = payload;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }
}
