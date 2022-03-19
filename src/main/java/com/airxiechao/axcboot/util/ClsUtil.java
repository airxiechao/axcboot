package com.airxiechao.axcboot.util;

import com.airxiechao.axcboot.communication.common.annotation.Required;
import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ClsUtil {

    public static <T> Set<Class<? extends T>> getSubTypesOf(String pkg, Class<T> type){
        Reflections reflections = new Reflections(pkg);
        Set<Class<? extends T>> subTypes = reflections.getSubTypesOf(type);
        return subTypes;
    }

    public static Set<Class<?>> getSubTypesOf(Set<Class<?>> classSet, Class<?> type){
        return classSet.stream().filter( cls -> type.isAssignableFrom(cls) ).collect(Collectors.toSet());
    }

    public static Set<Class<?>> getTypesAnnotatedWith(String pkg, Class<? extends Annotation> annotation){
        Reflections reflections = new Reflections(pkg);
        Set<Class<?>> annotated  = reflections.getTypesAnnotatedWith(annotation);
        return annotated;
    }

    public static Set<Field> getFields(Class type, Class<? extends Annotation> annotation){
        Set<Field> fieldSet = new HashSet<>();
        Class cls = type;
        while(Object.class != cls){
            Field[] fields = cls.getDeclaredFields();
            for (Field field : fields) {
                if(null == annotation || null != field.getAnnotation(annotation)){
                    fieldSet.add(field);
                }
            }

            cls = cls.getSuperclass();
        }

        return fieldSet;
    }

    public static Set<Method> getMethods(Class type, Class<? extends Annotation> annotation){
        Set<Method> methodSet = new HashSet<>();
        Class cls = type;
        while(Object.class != cls){
            Method[] methods = cls.getDeclaredMethods();
            for (Method method : methods) {
                if(null == annotation || null != AnnotationUtil.getMethodAnnotation(method, annotation)){
                    methodSet.add(method);
                }
            }

            cls = cls.getSuperclass();
        }

        return methodSet;
    }

    public static void checkRequiredField(Object obj){
        if(null == obj){
            throw new RuntimeException("check required field error: object is null");
        }
        Set<Field> requiredFields = getFields(obj.getClass(), Required.class);
        requiredFields.forEach(field -> field.setAccessible(true));

        requiredFields.forEach(field -> {
            Required required = field.getAnnotation(Required.class);
            if(null != required){
                String conditionalOnRequiredTrue = required.conditionalOnRequiredTrue();
                if(!StringUtil.isBlank(conditionalOnRequiredTrue)){
                    boolean conditionMet = false;
                    List<Field> conditionFields = requiredFields.stream().filter(f -> f.getName().equals(conditionalOnRequiredTrue)).collect(Collectors.toList());
                    for(Field f : conditionFields){
                        Object v = null;
                        try {
                            v = f.get(obj);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException("check condition field error", e);
                        }
                        if(null != v && v.equals(true)){
                            conditionMet = true;
                            break;
                        }
                    }

                    // if condition is not true, not check
                    if(!conditionMet){
                        return;
                    }
                }

                String name = field.getName();
                try {
                    Object value = field.get(obj);
                    if(null == value || (value instanceof String && StringUtil.isEmpty((String)value))){
                        throw new RuntimeException("field ["+name+"] is required");
                    }
                } catch (Exception e) {
                    throw new RuntimeException("field ["+name+"] is required", e);
                }
            }
        });
    }

    public static void checkRequiredField(Map<String, Object> obj, Class tClass){
        Set<Field> requiredFields = getFields(tClass, Required.class);
        requiredFields.forEach(field -> {
            Required required = field.getAnnotation(Required.class);
            if(null != required){
                field.setAccessible(true);
                String name = field.getName();
                Object value = obj.get(name);
                if(null == value || (value instanceof String && StringUtil.isEmpty((String)value))){
                    throw new RuntimeException("field ["+name+"] is required");
                }
            }
        });
    }

}
