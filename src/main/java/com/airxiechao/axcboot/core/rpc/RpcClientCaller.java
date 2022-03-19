package com.airxiechao.axcboot.core.rpc;

import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.common.annotation.Query;
import com.airxiechao.axcboot.communication.rpc.client.RpcClient;
import com.airxiechao.axcboot.util.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

public class RpcClientCaller {

    private RpcClient client;

    public RpcClientCaller(RpcClient client) {
        this.client = client;
    }

    public <T> T get(Class<T> cls) {
        return ProxyUtil.buildProxy(cls, (proxy, method, args) -> {
            try {
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

                Response response = client.callServer(type, param);

                Type retType = method.getGenericReturnType();
                JSONObject jsonObject = (JSONObject)JSON.toJSON(response);
                T resp = jsonObject.toJavaObject(retType);

                return resp;
            } catch (Exception e) {
                return new Response().error(e.getMessage());
            }
        });
    }
}
