package com.airxiechao.axcboot.core.biz;

import com.airxiechao.axcboot.core.annotation.IBiz;
import com.airxiechao.axcboot.util.ClsUtil;
import com.airxiechao.axcboot.util.lang.ImplReg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Set;

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
        for(Class cls : biz) {
            if (cls.isInterface()) {
                if(null != this.exclusion){
                    if(Arrays.stream(this.exclusion).anyMatch(e -> e == cls)){
                        continue;
                    }
                }

                Set<Class> set = ClsUtil.getSubTypesOf(this.pkg, cls);
                if(set.size() > 0){
                    Class impl = set.stream().findFirst().get();
                    logger.info("register biz: {}", impl);
                    this.registerImpl(cls, impl);
                }
            }
        }
    }
}
