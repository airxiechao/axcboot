package com.airxiechao.axcboot.storage.db.util;

import com.airxiechao.axcboot.storage.annotation.Table;
import com.airxiechao.axcboot.util.StringUtil;

import java.lang.reflect.Field;

public class DbUtil {

    public static String table(Class<?> tClass){
        Table table = tClass.getAnnotation(Table.class);
        if(null != table){
            return table.value();
        }

        return null;
    }

    public static String field(Class<?> tClass, String fieldName) {
        try {
            return field(tClass.getDeclaredField(fieldName));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static String field(Field field){
        return StringUtil.camelCaseToUnderscore(field.getName());
    }
}
