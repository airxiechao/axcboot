package com.airxiechao.axcboot.communication.rpc.common;

import io.netty.channel.ChannelHandlerContext;

public interface IRpcEventListener {
    void handle(ChannelHandlerContext ctx);
}
