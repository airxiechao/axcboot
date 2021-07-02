package com.airxiechao.axcboot.core.rest;

import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.rest.annotation.Get;
import com.airxiechao.axcboot.communication.rest.annotation.Post;
import com.airxiechao.axcboot.util.*;
import com.alibaba.fastjson.JSON;

import java.lang.reflect.Method;
import java.util.Map;

public abstract class AbstractRestClient {

    public <T> T getProxy(Class<T> cls, Map<String, String> headers, Map<String, String> cookies, int timeout){
        return ProxyUtil.buildProxy(cls, (proxy, method, args) -> {
            try{
                Object arg = args[0];
                Map<String, Object> params;
                if(arg instanceof Map){
                    params = (Map<String, Object>)arg;
                }else{
                    params = ModelUtil.toMap(arg, false);
                }
                String url = getUrl(method);

                String respString = null;

                Get get = AnnotationUtil.getMethodAnnotation(method, Get.class);
                if(null != get){
                    respString = HttpUtil.get(url, params, headers, cookies, timeout);
                }

                Post post = AnnotationUtil.getMethodAnnotation(method, Post.class);
                if(null != post){
                    respString = HttpUtil.postJson(url, params, headers, cookies,timeout);
                }

                if(StringUtil.isBlank(respString)){
                    return new Response().error();
                }

                Response resp = JSON.parseObject(respString, Response.class);

                return resp;
            }catch (Exception e){
                return new Response().error(e.getMessage());
            }
        });
    }

    public abstract String getUrl(Method method) throws Exception;
}
