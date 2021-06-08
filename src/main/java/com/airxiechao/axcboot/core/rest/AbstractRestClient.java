package com.airxiechao.axcboot.core.rest;

import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.rest.annotation.Get;
import com.airxiechao.axcboot.communication.rest.annotation.Post;
import com.airxiechao.axcboot.util.AnnotationUtil;
import com.airxiechao.axcboot.util.HttpUtil;
import com.airxiechao.axcboot.util.ProxyUtil;
import com.airxiechao.axcboot.util.StringUtil;
import com.alibaba.fastjson.JSON;

import java.lang.reflect.Method;
import java.util.Map;

public abstract class AbstractRestClient {

    public <T> T getProxy(Class<T> cls){
        return ProxyUtil.buildProxy(cls, (proxy, method, args) -> {
            try{
                Map params = (Map)args[0];
                String url = getUrl(method);

                String respString = null;

                Get get = AnnotationUtil.getMethodAnnotation(method, Get.class);
                if(null != get){
                    respString = HttpUtil.get(url, params, 0);
                }

                Post post = AnnotationUtil.getMethodAnnotation(method, Post.class);
                if(null != post){
                    respString = HttpUtil.postJson(url, params, 0);
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
