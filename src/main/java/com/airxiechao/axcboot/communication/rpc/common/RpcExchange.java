package com.airxiechao.axcboot.communication.rpc.common;

import io.netty.channel.ChannelHandlerContext;

import java.util.Map;

public class RpcExchange {
    private ChannelHandlerContext ctx;
    private Map<String, Object> payload;

    public RpcExchange(ChannelHandlerContext ctx, Map<String, Object> payload) {
        this.ctx = ctx;
        this.payload = payload;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public void setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}
