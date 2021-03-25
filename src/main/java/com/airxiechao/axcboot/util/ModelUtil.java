package com.airxiechao.axcboot.util;

import com.airxiechao.axcboot.storage.db.util.DbUtil;
import com.alibaba.fastjson.JSON;
import net.sf.cglib.beans.BeanCopier;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ModelUtil {

    public static void copyProperties(Object orig, Object dest){
        BeanCopier copier = BeanCopier.create(orig.getClass(), dest.getClass(), false);
        copier.copy(orig, dest, null);
    }

    public static <T> T deepCopy(T orig, Class<T> cls){
        return JSON.parseObject(JSON.toJSONString(orig), cls);
    }

    public static <T> T fromMap(Map map, Class<T> cls, boolean underscoreToCamelCase) throws Exception {
        return fromMap(map, cls, underscoreToCamelCase, null);
    }

    public static <T> T fromMap(Map map, Class<T> cls, boolean underscoreToCamelCase, String timeFormat) throws Exception {
        T obj = cls.getConstructor().newInstance();

        Field[] fields = cls.getDeclaredFields();
        for(Field field : fields){
            if(Modifier.isStatic(field.getModifiers())){
                continue;
            }

            field.setAccessible(true);

            String fieldName = underscoreToCamelCase ? DbUtil.field(field) : field.getName();
            Object fieldValue = map.get(fieldName);

            if(field.getType().equals(Integer.class) && fieldValue instanceof String){
                // string -> int
                fieldValue = Integer.valueOf((String)fieldValue);
            }else if(field.getType().equals(Long.class) && fieldValue instanceof String){
                // string -> long
                fieldValue = Long.valueOf((String)fieldValue);
            }else if(field.getType().equals(Double.class) && fieldValue instanceof String){
                // string -> double
                fieldValue = Double.valueOf((String)fieldValue);
            }else if(field.getType().equals(Date.class) && fieldValue instanceof String && null != timeFormat){
                // string -> time
                fieldValue = TimeUtil.fromStr((String)fieldValue, timeFormat);
            }

            field.set(obj, fieldValue);
        }

        return obj;
    }

    public static <T> Map<String, Object> toMap(T obj, boolean camelCaseToUnderscore) throws Exception {
        Map<String, Object> map = new HashMap<>();

        Field[] fields = obj.getClass().getDeclaredFields();
        for(Field field : fields){
            if(Modifier.isStatic(field.getModifiers())){
                continue;
            }

            field.setAccessible(true);

            String fieldName = camelCaseToUnderscore ? DbUtil.field(field) : field.getName();
            Object fieldValue = field.get(obj);
            map.put(fieldName, fieldValue);
        }

        return map;
    }
}
