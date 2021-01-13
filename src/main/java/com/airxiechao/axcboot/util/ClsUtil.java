package com.airxiechao.axcboot.util;

import org.reflections.Reflections;

import java.util.Set;

public class ClsUtil {

    public static <T> Set<Class<? extends T>> getSubTypesOf(String pkg, Class<T> type){
        Reflections reflections = new Reflections(pkg);
        Set<Class<? extends T>> subTypes = reflections.getSubTypesOf(type);
        return subTypes;
    }

    public static <T> Set<Class<? extends T>> getTypesAnnotatedWith(String pkg, Class<T> annotation){
        Reflections reflections = new Reflections(pkg);
        Set<Class<? extends T>> annotated  = reflections.getSubTypesOf(annotation);
        return annotated;
    }

}
