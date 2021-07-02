package com.airxiechao.axcboot.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AnnotationUtil {

    public static Annotation[] getClassAnnotations(Class cls) {
        List<Annotation> annotationList = new ArrayList<>();
        annotationList.addAll(Arrays.asList(cls.getAnnotations()));

        Class[] interfaces = cls.getInterfaces();
        for (Class it : interfaces) {
            annotationList.addAll(Arrays.asList(it.getAnnotations()));
        }

        return annotationList.stream().toArray(Annotation[]::new);
    }

    public static <T extends Annotation> T getClassAnnotations(Class cls, Class<T> annotationClass) {
        T annotation = (T)cls.getAnnotation(annotationClass);
        if(null != annotation){
            return annotation;
        }

        Annotation[] annotations = getClassAnnotations(cls);
        for(Annotation ann : annotations){
            if(ann.annotationType() == annotationClass){
                return (T)ann;
            }
        }

        return null;
    }

    public static Annotation[] getMethodAnnotations(Method method) {
        List<Annotation> annotationList = new ArrayList<>();
        annotationList.addAll(Arrays.asList(method.getAnnotations()));

        Class[] interfaces = method.getDeclaringClass().getInterfaces();
        for (Class it : interfaces) {
            try {
                Method mt = it.getMethod(method.getName(), method.getParameterTypes());
                annotationList.addAll(Arrays.asList(mt.getAnnotations()));
            } catch (Exception e) {

            }
        }

        return annotationList.stream().toArray(Annotation[]::new);
    }

    public static <T extends Annotation> T getMethodAnnotation(Method method, Class<T> annotationClass) {
        T annotation = method.getAnnotation(annotationClass);
        if(null != annotation){
            return annotation;
        }

        Annotation[] annotations = getMethodAnnotations(method);
        for(Annotation ann : annotations){
            if(ann.annotationType() == annotationClass){
                return (T)ann;
            }
        }

        return null;
    }
}
