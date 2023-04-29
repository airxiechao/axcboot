package com.airxiechao.axcboot.communication.rpc.common;

public class RpcMessage {

    public static final String TYPE_PING = "ping";

    public static final String RESPONSE_SUFFIX = "_response";

    private String requestId;
    private String type;
    private String payload;

    public RpcMessage(String requestId, String type, String payload){

        this.requestId = requestId;
        this.type = type;
        this.payload = payload;
    }

    public boolean isResponse(){
        return type.endsWith(RESPONSE_SUFFIX);
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

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
