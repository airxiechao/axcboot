package com.airxiechao.axcboot.communication.rpc.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.List;

public class RpcMessageDecoder extends ReplayingDecoder<RpcMessage> {


    private static Logger logger = LoggerFactory.getLogger(RpcMessageDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        String requestId = readStr(byteBuf);
        String type = readStr(byteBuf);
        //String content = URLDecoder.decode(readStr(byteBuf), "UTF-8");
        String content = readStr(byteBuf);
        list.add(new RpcMessage(requestId, type, content));
    }

    private String readStr(ByteBuf in) {
        int len = in.readInt();
        byte[] bytes = new byte[len];
        in.readBytes(bytes);
        return new String(bytes, Charset.forName("UTF-8"));
    }
}
