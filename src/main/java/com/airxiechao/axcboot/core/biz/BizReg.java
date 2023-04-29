package com.airxiechao.axcboot.core.biz;

import com.airxiechao.axcboot.core.annotation.IBiz;
import com.airxiechao.axcboot.util.ClsUtil;
import com.airxiechao.axcboot.util.ProxyUtil;
import com.airxiechao.axcboot.util.lang.ImplReg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class BizReg extends ImplReg {

    private static final Logger logger = LoggerFactory.getLogger(BizReg.class);

    private String pkg;
    private Class[] exclusion;

    public BizReg(String pkg){
        this.pkg = pkg;
    }

    public BizReg(String pkg, Class[] exclusion){
        this.pkg = pkg;
        this.exclusion = exclusion;
    }

    public void registerProcessIfExists(Class[] interfaces){
        for(Class cls : interfaces){
            Set<Class> set = ClsUtil.getSubTypesOf(this.pkg, cls);
            if(set.size() > 0){
                Class impl = set.stream().findFirst().get();
                logger.info("register biz: {}", impl);
                this.registerImpl(cls, impl);
            }
        }
    }

    public void registerProcessIfExists(){
        Set<Class<?>> biz = ClsUtil.getTypesAnnotatedWith(this.pkg, IBiz.class);
        Set<Class<?>> interfaceSet = biz.stream().filter( cls -> cls.isInterface()).collect(Collectors.toSet());
        Set<Class<?>> implSet = biz.stream().filter( cls -> !cls.isInterface()).collect(Collectors.toSet());

        for(Class cls : interfaceSet) {
            if(null != this.exclusion){
                if(Arrays.stream(this.exclusion).anyMatch(e -> e == cls)){
                    continue;
                }
            }

            Set<Class<?>> set = ClsUtil.getSubTypesOf(implSet, cls);
            if(set.size() > 0){
                Class impl = set.stream().findFirst().get();
                logger.info("register biz: {}", impl);
                this.registerImpl(cls, impl);
            }
        }
    }

    public static <T> T getBizImplProxy(Supplier<? extends BizReg> regSupplier, Class<T> interfaceCls){
        return ProxyUtil.buildProxy(interfaceCls, (proxy, method, args) -> {
            BizReg reg = regSupplier.get();
            T implObj = reg.getImpl(interfaceCls);

            if(null == implObj){
                logger.info("try register biz: {}", interfaceCls);
                reg.registerProcessIfExists(new Class[]{ interfaceCls });
                implObj = reg.getImpl(interfaceCls);
            }

            if(null == implObj){
                throw new RuntimeException("no implementation of biz：" + interfaceCls);
            }

            try{
                return method.invoke(implObj, args);
            }catch (InvocationTargetException e){
                throw e.getCause();
            }
        });
    }
}
