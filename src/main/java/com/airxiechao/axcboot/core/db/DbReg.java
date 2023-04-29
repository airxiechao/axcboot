package com.airxiechao.axcboot.core.db;

import com.airxiechao.axcboot.core.annotation.IDb;
import com.airxiechao.axcboot.core.biz.BizReg;
import com.airxiechao.axcboot.storage.db.sql.DbManager;
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

public class DbReg extends ImplReg {

    private static final Logger logger = LoggerFactory.getLogger(DbReg.class);

    private String pkg;
    private Function<Class, DbManager> dbManagerSupplier;
    private Class[] exclusion;

    public DbReg(String pkg, Function<Class, DbManager> dbManagerSupplier){
        this.pkg = pkg;
        this.dbManagerSupplier = dbManagerSupplier;
    }

    public DbReg(String pkg, Function<Class, DbManager> dbManagerSupplier, Class[] exclusion){
        this.pkg = pkg;
        this.dbManagerSupplier = dbManagerSupplier;
        this.exclusion = exclusion;
    }

    public void registerProcedureIfExists(Class[] interfaces){
        for(Class cls : interfaces){
            Set<Class> set = ClsUtil.getSubTypesOf(this.pkg, cls);
            if(set.size() > 0){
                Class impl = set.stream().findFirst().get();
                logger.info("register db: {}", impl);
                this.registerProcedure(cls, impl);
            }
        }
    }

    public void registerProcedureIfExists(){
        Set<Class<?>> db = ClsUtil.getTypesAnnotatedWith(this.pkg, IDb.class);
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
                logger.info("register db: {}", impl);
                this.registerProcedure(cls, impl);
            }
        }
    }

    protected <T> void registerProcedure(Class<T> interfaceCls, Class<? extends T> implCls){
        try {
            T impl = implCls.getConstructor(DbManager.class).newInstance(this.dbManagerSupplier.apply(interfaceCls));
            registerImpl(interfaceCls, impl);
        } catch (Exception e){
            logger.error("register db procedure error", e);
        }
    }

    public static <T> T getDbImplProxy(Supplier<? extends DbReg> regSupplier, Class<T> interfaceCls){
        return ProxyUtil.buildProxy(interfaceCls, (proxy, method, args) -> {
            DbReg reg = regSupplier.get();
            T implObj = reg.getImpl(interfaceCls);

            if(null == implObj){
                logger.info("try register db: {}", interfaceCls);
                reg.registerProcedureIfExists(new Class[]{ interfaceCls });
                implObj = reg.getImpl(interfaceCls);
            }

            if(null == implObj){
                throw new RuntimeException("no implementation of db：" + interfaceCls);
            }

            try{
                return method.invoke(implObj, args);
            }catch (InvocationTargetException e){
                throw e.getCause();
            }
        });
    }
}
