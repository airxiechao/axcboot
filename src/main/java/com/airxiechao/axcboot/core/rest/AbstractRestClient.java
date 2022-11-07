package com.airxiechao.axcboot.core.rest;

import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.rest.annotation.Get;
import com.airxiechao.axcboot.communication.rest.annotation.Post;
import com.airxiechao.axcboot.communication.rest.constant.EnumContentType;
import com.airxiechao.axcboot.util.*;
import com.airxiechao.axcboot.util.HttpUtil;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public abstract class AbstractRestClient {

    private static final Logger logger = LoggerFactory.getLogger(AbstractRestClient.class);

    public <T> T getProxy(Class<T> cls, String serviceTag,
                          Map<String, String> headers, Map<String, String> cookies, int timeout, boolean useSsl,
                          BiConsumer<Long, Long> totalAndSpeedConsumer, Supplier<Boolean> stopSupplier){
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
                    respString = HttpUtil.get(url, params, headers, cookies, timeout, useSsl);
                }

                Post post = AnnotationUtil.getMethodAnnotation(method, Post.class);
                if(null != post){
                    switch (post.contentType()){
                        case EnumContentType.FORM_URL_ENCODED:
                            respString = HttpUtil.postFormUrlEncoded(url, params, headers, cookies, timeout, useSsl);
                            break;
                        case EnumContentType.FORM_MULTIPART:
                            respString = HttpUtil.postFormMultipart(url, params, headers, cookies, timeout, useSsl, totalAndSpeedConsumer, stopSupplier);
                            break;
                        default:
                            respString = HttpUtil.postJson(url, params, headers, cookies, timeout, useSsl);
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
                          Map<String, String> headers, Map<String, String> cookies, int timeout, boolean useSsl){
        return getProxy(cls, serviceTag, headers, cookies, timeout, useSsl, null, null);
    }

    public <T> T getProxy(Class<T> cls, String serviceTag,
                          Map<String, String> headers, Map<String, String> cookies, int timeout, boolean useSsl,
                          OutputStream outputStream, BiConsumer<Long, Long> totalAndSpeedConsumer, Supplier<Boolean> stopSupplier
    ){
        return ProxyUtil.buildProxy(cls, (proxy, method, args) -> {
            if(!method.getReturnType().equals(void.class)){
                return getProxy(cls, serviceTag, headers, cookies, timeout, useSsl, totalAndSpeedConsumer, stopSupplier);
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

                HttpUtil.download(url, params, headers, cookies, timeout, useSsl, outputStream, totalAndSpeedConsumer, stopSupplier);
            }catch (Exception e){
                logger.error("rest client download error", e);
            }

            return null;
        });
    }

    public abstract String getUrl(Method method, String serviceTag) throws Exception;
}
