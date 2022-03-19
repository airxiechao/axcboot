package com.airxiechao.axcboot.communication.rpc.common;

import io.netty.channel.ChannelHandlerContext;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RpcContextContainer {

    private Map<String, RpcContext> mapClientRpcContext = new ConcurrentHashMap<>();
    private Map<ChannelHandlerContext, String> mapChannelContextClient = new ConcurrentHashMap<>();

    public void put(String client, RpcContext rpcContext){
        mapClientRpcContext.put(client, rpcContext);
        mapChannelContextClient.put(rpcContext.getContext(), client);
    }

    public boolean containsClient(String client){
        return mapClientRpcContext.containsKey(client);
    }

    public RpcContext getRpcContextByClient(String client){
        return mapClientRpcContext.get(client);
    }

    public String getClientByChannelContext(ChannelHandlerContext ctx){
        return mapChannelContextClient.get(ctx);
    }

    public Set<Map.Entry<String, RpcContext>> clientRpcContextEntrySet(){
        return mapClientRpcContext.entrySet();
    }

    public void removeByClient(String client){
        RpcContext rpcContext = mapClientRpcContext.get(client);
        if(null != rpcContext){
            mapChannelContextClient.remove(rpcContext.getContext());
            mapClientRpcContext.remove(client);
        }
    }
}
