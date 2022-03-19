package com.airxiechao.axcboot.communication.rpc.common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;

@Sharable
public class RpcMessageEncoder extends MessageToMessageEncoder<RpcMessage> {

    private static Logger logger = LoggerFactory.getLogger(RpcMessageEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, RpcMessage message, List<Object> list) throws Exception {
        ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer();
        writeStr(buf, message.getRequestId());
        writeStr(buf, message.getType());
        //writeStr(buf, URLEncoder.encode(message.getPayload(), "UTF-8"));
        writeStr(buf, message.getPayload());
        list.add(buf);
    }

    private void writeStr(ByteBuf buf, String s) {
        byte[] bytes = s.getBytes(Charset.forName("UTF-8"));
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }
}
