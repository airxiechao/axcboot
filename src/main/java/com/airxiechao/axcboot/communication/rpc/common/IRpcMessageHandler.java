package com.airxiechao.axcboot.communication.rpc.common;

import com.airxiechao.axcboot.communication.common.Response;
import io.netty.channel.ChannelHandlerContext;

import java.util.Map;

public interface IRpcMessageHandler {

    Response handle(ChannelHandlerContext ctx, Map<String, Object> payload) throws Exception;
}
