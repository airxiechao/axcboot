package com.airxiechao.axcboot.core.db;

import com.airxiechao.axcboot.core.annotation.IDb;
import com.airxiechao.axcboot.storage.db.DbManager;
import com.airxiechao.axcboot.util.ClsUtil;
import com.airxiechao.axcboot.util.lang.ImplReg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;

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
        for(Class cls : db) {
            if (cls.isInterface()) {
                if(null != this.exclusion){
                    if(Arrays.stream(this.exclusion).anyMatch(e -> e == cls)){
                        continue;
                    }
                }

                Set<Class> set = ClsUtil.getSubTypesOf(this.pkg, cls);
                if(set.size() > 0){
                    Class impl = set.stream().findFirst().get();
                    logger.info("register db: {}", impl);
                    this.registerProcedure(cls, impl);
                }
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
}
