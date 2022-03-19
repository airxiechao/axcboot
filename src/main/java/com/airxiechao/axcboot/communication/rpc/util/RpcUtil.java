package com.airxiechao.axcboot.communication.rpc.util;

import com.airxiechao.axcboot.communication.common.annotation.Auth;
import com.airxiechao.axcboot.communication.common.annotation.Param;
import com.airxiechao.axcboot.communication.common.annotation.Params;
import com.airxiechao.axcboot.communication.common.security.IAuthTokenChecker;
import com.airxiechao.axcboot.communication.rpc.common.IRpcMessageHandler;
import com.airxiechao.axcboot.communication.rpc.common.RpcExchange;
import com.airxiechao.axcboot.communication.rpc.server.RpcServer;
import com.airxiechao.axcboot.util.ModelUtil;
import com.airxiechao.axcboot.util.ClsUtil;
import com.airxiechao.axcboot.util.AnnotationUtil;
import com.airxiechao.axcboot.util.StringUtil;
import com.airxiechao.axcboot.util.TimeUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.ChannelHandlerContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.text.ParseException;
import java.util.*;

import static com.airxiechao.axcboot.communication.rpc.common.RpcMessage.RESPONSE_SUFFIX;

public class RpcUtil {

    /**
     * 构造响应类型
     * @param requestType
     * @return
     */
    public static String buildResponseType(String requestType){
        return requestType + RESPONSE_SUFFIX;
    }

    /**
     * 获取对方IP
     * @param ctx
     * @return
     */
    public static String getRemoteIp(ChannelHandlerContext ctx){
        return ((InetSocketAddress)ctx.channel().remoteAddress()).getAddress().getHostAddress();
    }

    /**
     * 获取客户端名称
     * @param rpcServer
     * @param ctx
     * @return
     */
    public static String getClient(RpcServer rpcServer, ChannelHandlerContext ctx){
        return rpcServer.getRouter().getClientByContext(ctx);
    }

    /**
     * 得到处理方法
     * @param cls
     * @return
     */
    public static Method getHandleMethod(Class<? extends IRpcMessageHandler> cls) {
        try {
            Method method = IRpcMessageHandler.class.getDeclaredMethods()[0];
            return cls.getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * 检查参数
     * @param method
     * @throws Exception
     */
    public static void checkParameter(Method method, Map<String, Object> queryParams) throws Exception {
        Annotation[] methodAnnos = AnnotationUtil.getMethodAnnotations(method);
        List<Param> params = new ArrayList<>();
        for(Annotation anno : methodAnnos){
            if(anno instanceof Params){
                params.addAll(Arrays.asList(((Params) anno).value()));
            }else if(anno instanceof Param){
                Param param = (Param)anno;
                params.add(param);
            }
        }

        for(Param param : params){
            String name = param.value();
            boolean required = param.required();


            if(required){
                if(null == queryParams){
                    throw new Exception("parameter [" + name + "] is required");
                }

                Object value = queryParams.get(name);

                if(null == value){
                    throw new Exception("parameter [" + name + "] is required");
                }
            }
        }
    }

    /**
     * 获取authToken
     * @param queryParams
     * @return
     */
    public static String getAuthToken(Map<String, Object> queryParams){
        if(null == queryParams){
            return null;
        }

        String authToken = (String)queryParams.get("auth");
        return authToken;
    }

    public static String getAuthToken(RpcExchange rpcExchange){
        return getAuthToken(rpcExchange.getPayload());
    }

    /**
     * 检查权限
     * @param method
     * @throws Exception
     */
    public static void checkAuth(Method method, ChannelHandlerContext ctx, Map<String, Object> queryParams, IAuthTokenChecker rpcAuthChecker) throws Exception {
        Auth auth = AnnotationUtil.getMethodAnnotation(method, Auth.class);
        if(null == auth){
            return;
        }

        if(null == rpcAuthChecker){
            throw new Exception("no auth checker");
        }

        String authToken = getAuthToken(queryParams);
        if(StringUtil.isEmpty(authToken)){
            throw new Exception("auth token is empty");
        }

        String scope = auth.scope();
        String item = auth.item();
        int mode = auth.mode();

        boolean passed = rpcAuthChecker.validate(authToken, scope, item, mode);
        if(!passed){
            throw new Exception("auth check fails");
        }
    }

    /**
     * 提取参数
     * @param map
     * @param key
     * @param <T>
     * @return
     */
    public static <T> T getParam(Map<String, Object> map, String key){
        return (T)map.get(key);
    }

    public static <T> T getParam(Map<String, Object> map, String key, T defaultValue){
        T value = getParam(map, key);
        if(null == value){
            value = defaultValue;
        }
        return value;
    }

    public static String getStringParam(Map<String, Object> map, String key){
        return String.valueOf(map.get(key));
    }

    public static Integer getIntegerParam(Map<String, Object> map, String key){
        return Integer.valueOf(getStringParam(map, key));
    }

    public static Long getLongParam(Map<String, Object> map, String key){
        return Long.valueOf(getStringParam(map, key));
    }

    public static Date getDateParam(Map<String, Object> map, String key) throws ParseException {
        return TimeUtil.toDate(getStringParam(map, key));
    }

    public static Date getTimeParam(Map<String, Object> map, String key) throws ParseException {
        return TimeUtil.toTime(getStringParam(map, key));
    }


    public static <T> T getParam(Map<String, Object> map, String key, Class<T> cls){
        Object obj = map.get(key);
        return JSON.parseObject(JSON.toJSONString(obj), cls);
    }

    public static <T> T getParam(Map<String, Object> map, String key, Class<T> cls, T defaultValue){
        T value = getParam(map, key, cls);
        if(null == value){
            value = defaultValue;
        }
        return value;
    }

    public static <T> T getObjectParam(RpcExchange rpcExchange, Class<T> cls){
        T obj = ModelUtil.fromMap(rpcExchange.getPayload(), cls);
        ClsUtil.checkRequiredField(obj);
        return obj;
    }
}
