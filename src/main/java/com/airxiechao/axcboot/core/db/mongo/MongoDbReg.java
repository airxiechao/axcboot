package com.airxiechao.axcboot.core.db.mongo;

import com.airxiechao.axcboot.core.annotation.IMongoDb;
import com.airxiechao.axcboot.storage.db.mongodb.MongoDbManager;
import com.airxiechao.axcboot.util.ClsUtil;
import com.airxiechao.axcboot.util.ProxyUtil;
import com.airxiechao.axcboot.util.lang.ImplReg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MongoDbReg extends ImplReg {

    private static final Logger logger = LoggerFactory.getLogger(MongoDbReg.class);

    private String pkg;
    private Function<Class, MongoDbManager> dbManagerSupplier;
    private Class[] exclusion;

    public MongoDbReg(String pkg, Function<Class, MongoDbManager> dbManagerSupplier){
        this.pkg = pkg;
        this.dbManagerSupplier = dbManagerSupplier;
    }

    public MongoDbReg(String pkg, Function<Class, MongoDbManager> dbManagerSupplier, Class[] exclusion){
        this.pkg = pkg;
        this.dbManagerSupplier = dbManagerSupplier;
        this.exclusion = exclusion;
    }

    public void registerProcedureIfExists(Class[] interfaces){
        for(Class cls : interfaces){
            Set<Class> set = ClsUtil.getSubTypesOf(this.pkg, cls);
            if(set.size() > 0){
                Class impl = set.stream().findFirst().get();
                logger.info("register mongodb: {}", impl);
                this.registerProcedure(cls, impl);
            }
        }
    }

    public void registerProcedureIfExists(){
        Set<Class<?>> db = ClsUtil.getTypesAnnotatedWith(this.pkg, IMongoDb.class);
        Set<Class<?>> interfaceSet = db.stream().filter( cls -> cls.isInterface()).collect(Collectors.toSet());
        Set<Class<?>> implSet = db.stream().filter( cls -> !cls.isInterface()).collect(Collectors.toSet());


        for(Class cls : interfaceSet) {
            if(null != this.exclusion){
                if(Arrays.stream(this.exclusion).anyMatch(e -> e == cls)){
                    continue;
                }
            }

            Set<Class<?>> set = ClsUtil.getSubTypesOf(implSet, cls);
            if(set.size() > 0){
                Class impl = set.stream().findFirst().get();
                logger.info("register mongodb: {}", impl);
                this.registerProcedure(cls, impl);
            }
        }
    }

    protected <T> void registerProcedure(Class<T> interfaceCls, Class<? extends T> implCls){
        try {
            T impl = implCls.getConstructor(MongoDbManager.class).newInstance(this.dbManagerSupplier.apply(interfaceCls));
            registerImpl(interfaceCls, impl);
        } catch (Exception e){
            logger.error("register mongodb procedure error", e);
        }
    }

    public static <T> T getMongoDbImplProxy(Supplier<? extends MongoDbReg> regSupplier, Class<T> interfaceCls){
        return ProxyUtil.buildProxy(interfaceCls, (proxy, method, args) -> {
            MongoDbReg reg = regSupplier.get();
            T implObj = reg.getImpl(interfaceCls);

            if(null == implObj){
                logger.info("try register mongodb: {}", interfaceCls);
                reg.registerProcedureIfExists(new Class[]{ interfaceCls });
                implObj = reg.getImpl(interfaceCls);
            }

            if(null == implObj){
                throw new RuntimeException("no implementation of mongodb：" + interfaceCls);
            }

            try{
                return method.invoke(implObj, args);
            }catch (InvocationTargetException e){
                throw e.getCause();
            }
        });
    }
}