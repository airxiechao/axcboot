package com.airxiechao.axcboot.core.rest;

import com.airxiechao.axcboot.communication.rest.server.RestServer;
import com.airxiechao.axcboot.communication.websocket.common.AbstractWsListener;
import com.airxiechao.axcboot.core.annotation.IRest;
import com.airxiechao.axcboot.util.ClsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class RestReg {

    private static final Logger logger = LoggerFactory.getLogger(RestReg.class);

    private String pkg;
    private RestServer restServer;
    private Class[] exclusion;

    public RestReg(String pkg, RestServer restServer){
        this.pkg = pkg;
        this.restServer = restServer;
    }

    public RestReg(String pkg, RestServer restServer, Class[] exclusion){
        this.pkg = pkg;
        this.restServer = restServer;
        this.exclusion = exclusion;
    }

    public void registerHandlerIfExists(Class[] interfaces){
        for(Class cls : interfaces){
            Set<Class> set = ClsUtil.getSubTypesOf(this.pkg, cls);
            if(set.size() > 0){
                Class impl = set.stream().findFirst().get();
                logger.info("register rest: {}", impl);
                restServer.registerHandler(impl);
                this.afterRegisterHandler(impl, restServer);
            }
        }
    }

    public void registerHandlerIfExists(){
        Set<Class<?>> rest = ClsUtil.getTypesAnnotatedWith(this.pkg, IRest.class);
        Set<Class<?>> interfaceSet = rest.stream().filter( cls -> cls.isInterface()).collect(Collectors.toSet());
        Set<Class<?>> implSet = rest.stream().filter( cls -> !cls.isInterface()).collect(Collectors.toSet());

        for(Class cls : interfaceSet){
            if(null != this.exclusion){
                if(Arrays.stream(this.exclusion).anyMatch(e -> e == cls)){
                    continue;
                }
            }

            Set<Class<?>> set = ClsUtil.getSubTypesOf(implSet, cls);
            if(set.size() > 0){
                Class impl = set.stream().findFirst().get();
                logger.info("register rest: {}", impl);
                restServer.registerHandler(impl);
                this.afterRegisterHandler(impl, restServer);
            }

        }
    }

    public void registerWsIfExists(Class[] interfaces){
        for(Class api : interfaces){
            Set<Class> set = ClsUtil.getSubTypesOf(this.pkg, api);
            if(set.size() > 0) {
                Class impl = set.stream().findFirst().get();
                logger.info("register ws: {}", impl);
                restServer.registerWs(impl);
            }
        }
    }

    public void registerWsIfExists(){
        Set<Class<? extends AbstractWsListener>> set = ClsUtil.getSubTypesOf(this.pkg, AbstractWsListener.class);
        for(Class<? extends AbstractWsListener> ws : set){
            if(!Modifier.isAbstract(ws.getModifiers())){
                logger.info("register ws: {}", ws);
                restServer.registerWs(ws);
            }
        }
    }

    protected void afterRegisterHandler(Class<?> cls, RestServer restServer) {

    };
}
