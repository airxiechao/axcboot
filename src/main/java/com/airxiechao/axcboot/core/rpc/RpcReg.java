package com.airxiechao.axcboot.core.rpc;

import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.common.annotation.Query;
import com.airxiechao.axcboot.communication.rpc.client.RpcClient;
import com.airxiechao.axcboot.communication.rpc.common.IRpcMessageHandler;
import com.airxiechao.axcboot.communication.rpc.server.RpcServer;
import com.airxiechao.axcboot.core.annotation.IRpc;
import com.airxiechao.axcboot.util.AnnotationUtil;
import com.airxiechao.axcboot.util.ClsUtil;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RpcReg {
    private static final Logger logger = LoggerFactory.getLogger(RpcReg.class);

    private String pkg;
    private RpcServer rpcServer;
    private RpcClient rpcClient;
    private Class[] exclusion;

    public RpcReg(String pkg, RpcServer rpcServer){
        this.pkg = pkg;
        this.rpcServer = rpcServer;
    }

    public RpcReg(String pkg, RpcClient rpcClient){
        this.pkg = pkg;
        this.rpcClient = rpcClient;
    }

    public RpcReg(String pkg, RpcClient rpcClient, Class[] exclusion){
        this.pkg = pkg;
        this.rpcClient = rpcClient;
        this.exclusion = exclusion;
    }

    public void registerHandlerIfExists(Function<Class<?>, Object> handlerSupplier){
        Set<Class<?>> rpc = ClsUtil.getTypesAnnotatedWith(this.pkg, IRpc.class);
        Set<Class<?>> interfaceSet = rpc.stream().filter( cls -> cls.isInterface()).collect(Collectors.toSet());
        Set<Class<?>> implSet = rpc.stream().filter( cls -> !cls.isInterface()).collect(Collectors.toSet());

        for(Class cls : interfaceSet){
            if(null != this.exclusion){
                if(Arrays.stream(this.exclusion).anyMatch(e -> e == cls)){
                    continue;
                }
            }

            Set<Class<?>> set = ClsUtil.getSubTypesOf(implSet, cls);
            if(set.size() > 0){
                Class impl = set.stream().findFirst().get();
                logger.info("register rpc: {}", impl);

                try{
                    Object handler = null;
                    if(null == handlerSupplier){
                        Constructor constructor = impl.getDeclaredConstructor();
                        constructor.setAccessible(true);
                        handler = constructor.newInstance();
                    }else{
                        handler = handlerSupplier.apply(impl);
                    }
                    Object finalHandler = handler;
                    Set<Method> methods = ClsUtil.getMethods(impl, Query.class);
                    methods.forEach(method -> {
                        if(null != rpcServer){
                            rpcServer.registerRpcHandler(finalHandler, method);
                        }
                        if(null != rpcClient){
                            rpcClient.registerRpcHandler(finalHandler, method);
                        }
                    });
                }catch (Exception e){
                    logger.error("register rpc [{}] error", e);
                    continue;
                }
            }
        }
    }
}
