package com.airxiechao.axcboot.communication.rpc.security;

import io.netty.channel.ChannelHandlerContext;

public interface IRpcAuthChecker {

    boolean check(ChannelHandlerContext ctx, String authToken);
}
