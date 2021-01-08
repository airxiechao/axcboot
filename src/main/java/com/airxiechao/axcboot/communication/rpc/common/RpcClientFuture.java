package com.airxiechao.axcboot.communication.rpc.common;

public class RpcClientFuture extends RpcFuture {

    private String clientName;

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }
}
