package com.airxiechao.axcboot.util;

import com.airxiechao.axcboot.storage.db.sql.util.DbUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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

    public static <T> T fromMap(Map map, Class<T> cls){
        JSONObject json = new JSONObject(map);
        return json.toJavaObject(cls);
    }

    public static <T> T fromMapAndCheckRequiredField(Map map, Class<T> cls) {
        T obj = fromMap(map, cls);
        ClsUtil.checkRequiredField(obj);
        return obj;
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

            String fieldName = underscoreToCamelCase ? DbUtil.column(field) : field.getName();
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

    public static Map<String, Object> toMap(Object obj){
        return (JSONObject)JSON.toJSON(obj);
    }

    public static Map<String, Object> toMapAndCheckRequiredField(Object obj){
        ClsUtil.checkRequiredField(obj);
        return (JSONObject)JSON.toJSON(obj);
    }

    public static Map<String, String> toStringMap(Object obj){
        Map<String, String> stringMap = new HashMap<>();
        JSONObject jsonObject = (JSONObject)JSON.toJSON(obj);
        jsonObject.entrySet().forEach(entry -> {
            String key = entry.getKey();
            Object value = entry.getValue();
            if(null != value){
                stringMap.put(key, value.toString());
            }else{
                stringMap.put(key, "");
            }
        });

        return stringMap;
    }

    public static Map<String, String> toStringMapAndCheckRequiredField(Object obj){
        ClsUtil.checkRequiredField(obj);
        return toStringMap(obj);
    }

    public static <T> Map<String, T> toMap(Object obj, boolean camelCaseToUnderscore) {
        Map<String, T> map = new HashMap<>();

        Field[] fields = obj.getClass().getDeclaredFields();
        for(Field field : fields){
            if(Modifier.isStatic(field.getModifiers())){
                continue;
            }

            field.setAccessible(true);

            String fieldName = camelCaseToUnderscore ? DbUtil.column(field) : field.getName();
            Object fieldValue = null;
            try {
                fieldValue = field.get(obj);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            map.put(fieldName, (T)fieldValue);
        }

        return map;
    }
}
