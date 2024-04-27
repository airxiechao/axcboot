package com.airxiechao.axcboot.core.rpc;

import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.common.annotation.Query;
import com.airxiechao.axcboot.communication.rpc.server.RpcServer;
import com.airxiechao.axcboot.util.*;

import java.util.Map;

public class RpcServerCaller {

    private RpcServer server;

    public RpcServerCaller(RpcServer server) {
        this.server = server;
    }

    public <T> T get(Class<T> cls, String client) {
        return ProxyUtil.buildProxy(cls, (proxy, method, args) -> {
            try {
                if(StringUtil.isBlank(client)){
                    throw new Exception("no client");
                }

                Object arg = args[0];
                Map<String, Object> param;
                if (arg instanceof Map) {
                    param = (Map<String, Object>) arg;
                } else {
                    param = ModelUtil.toMap(arg);
                }

                Query query = method.getAnnotation(Query.class);
                if(null == query){
                    throw new Exception("unknown rpc type");
                }

                String type = query.value();

                Response resp = server.callClient(client, type, param);
                return resp;
            } catch (Exception e) {
                return new Response().error(e.getMessage());
            }
        });
    }
}
