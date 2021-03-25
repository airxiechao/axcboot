package com.airxiechao.axcboot.communication.rpc.common;

import io.netty.channel.ChannelHandlerContext;

public interface IRpcClientListener {
    void handle(ChannelHandlerContext ctx, String client);
}
