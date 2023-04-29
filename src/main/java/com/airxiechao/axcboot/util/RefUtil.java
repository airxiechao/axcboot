package com.airxiechao.axcboot.util;

import com.airxiechao.axcboot.annotation.Ref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class RefUtil {

    private static final Logger logger = LoggerFactory.getLogger(RefUtil.class);

    public static <T> Map<String, Class<? extends T>> parseRefStrCls(Class<?> tableCls, boolean useName){
        Map<String, Class<? extends T>> map = new HashMap<>();
        for (Field f: tableCls.getDeclaredFields()) {
            f.setAccessible(true);
            Ref ref = f.getAnnotation(Ref.class);
            if(null != ref){
                Class<? extends T> cls = (Class<? extends T>)ref.value();
                try {
                    String name = useName ? f.getName() : (String)f.get(null);
                    map.put(name, cls);
                } catch (Exception e) {
                    logger.error("parse ref [{}] error", tableCls.toString(), e);
                }
            }
        }

        return map;
    }

    public static <T> Map<Class<? extends T>, String> parseRefClsStr(Class<?> tableCls, boolean useName){
        Map<Class<? extends T>, String> map = new HashMap<>();
        for (Field f: tableCls.getDeclaredFields()) {
            f.setAccessible(true);
            Ref ref = f.getAnnotation(Ref.class);
            if(null != ref){
                Class<? extends T> cls = (Class<? extends T>)ref.value();
                try {
                    String name = useName ? f.getName() : (String)f.get(null);
                    map.put(cls, name);
                } catch (Exception e) {
                    logger.error("parse ref [{}] error", tableCls.toString(), e);
                }
            }
        }

        return map;
    }

}
