package com.airxiechao.axcboot.communication.rpc.common;

import io.netty.channel.ChannelHandlerContext;

import java.util.Date;

public class RpcContext {

    public static final int HEARTBEAT_PERIOD_SECS = 60;

    private ChannelHandlerContext context;
    private Date lastHeartbeatTime;

    public ChannelHandlerContext getContext() {
        return context;
    }

    public void setContext(ChannelHandlerContext context) {
        this.context = context;
    }

    public Date getLastHeartbeatTime() {
        return lastHeartbeatTime;
    }

    public void setLastHeartbeatTime(Date lastHeartbeatTime) {
        this.lastHeartbeatTime = lastHeartbeatTime;
    }

    public boolean isHeartbeatExpired(){
        if(null == lastHeartbeatTime){
            return true;
        }

        long now = new Date().getTime();
        long heartbeat = lastHeartbeatTime.getTime();
        if(now > heartbeat + 2 * 1000 * HEARTBEAT_PERIOD_SECS){
            return true;
        }

        return false;
    }
}
