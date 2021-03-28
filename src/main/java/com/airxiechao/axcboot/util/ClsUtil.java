package com.airxiechao.axcboot.util;

import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.util.Set;

public class ClsUtil {

    public static <T> Set<Class<? extends T>> getSubTypesOf(String pkg, Class<T> type){
        Reflections reflections = new Reflections(pkg);
        Set<Class<? extends T>> subTypes = reflections.getSubTypesOf(type);
        return subTypes;
    }

    public static Set<Class<?>> getTypesAnnotatedWith(String pkg, Class<? extends Annotation> annotation){
        Reflections reflections = new Reflections(pkg);
        Set<Class<?>> annotated  = reflections.getTypesAnnotatedWith(annotation);
        return annotated;
    }

}
