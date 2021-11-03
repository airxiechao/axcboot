package com.airxiechao.axcboot.core.rest;

import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.rest.annotation.Get;
import com.airxiechao.axcboot.communication.rest.annotation.Post;
import com.airxiechao.axcboot.communication.rest.constant.EnumContentType;
import com.airxiechao.axcboot.communication.rest.util.RestUtil;
import com.airxiechao.axcboot.util.*;
import com.alibaba.fastjson.JSON;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Random;

public abstract class AbstractRestClient {

    private static final Logger logger = LoggerFactory.getLogger(AbstractRestClient.class);

    public <T> T getProxy(Class<T> cls, String serviceTag,
                          Map<String, String> headers, Map<String, String> cookies, int timeout){
        return ProxyUtil.buildProxy(cls, (proxy, method, args) -> {
            try{
                Object arg = args[0];
                Map<String, Object> params;
                if(arg instanceof Map){
                    params = (Map<String, Object>)arg;
                }else{
                    params = ModelUtil.toMap(arg, false);
                }
                String url = getUrl(method, serviceTag);

                String respString = null;

                Get get = AnnotationUtil.getMethodAnnotation(method, Get.class);
                if(null != get){
                    respString = HttpUtil.get(url, params, headers, cookies, timeout);
                }

                Post post = AnnotationUtil.getMethodAnnotation(method, Post.class);
                if(null != post){
                    switch (post.contentType()){
                        case EnumContentType.FORM_URL_ENCODED:
                            respString = HttpUtil.postFormUrlEncoded(url, params, headers, cookies, timeout);
                            break;
                        case EnumContentType.FORM_MULTIPART:
                            respString = HttpUtil.postFormMultipart(url, params, headers, cookies, timeout);
                            break;
                        default:
                            respString = HttpUtil.postJson(url, params, headers, cookies,timeout);
                            break;
                    }
                }

                if(StringUtil.isBlank(respString)){
                    return new Response().error();
                }

                Type retType = method.getGenericReturnType();
                T resp = JSON.parseObject(respString, retType);
                if(resp instanceof Response){
                    Response response = (Response)resp;
                    if(!response.isSuccess()){
                        logger.error("call service [{}.{}] error: {}", cls.getName(), method.getName(), response.getMessage());
                    }
                }

                return resp;
            }catch (Exception e){
                logger.error("rest client error", e);
                return new Response().error(e.getMessage());
            }
        });
    }

    public <T> T getProxy(Class<T> cls, String serviceTag,
                          Map<String, String> headers, Map<String, String> cookies, int timeout,
                          OutputStream outputStream
    ){
        return ProxyUtil.buildProxy(cls, (proxy, method, args) -> {
            if(!method.getReturnType().equals(void.class)){
                return getProxy(cls, serviceTag, headers, cookies, timeout);
            }

            try{
                Object arg = args[0];
                Map<String, Object> params;
                if(arg instanceof Map){
                    params = (Map<String, Object>)arg;
                }else{
                    params = ModelUtil.toMap(arg, false);
                }
                String url = getUrl(method, serviceTag);

                HttpUtil.download(url, params, headers, cookies, timeout, outputStream);
            }catch (Exception e){
                logger.error("rest client download error", e);
            }

            return null;
        });
    }

    public abstract String getUrl(Method method, String serviceTag) throws Exception;
}
