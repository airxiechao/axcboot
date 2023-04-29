package com.airxiechao.axcboot.storage.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
    String name() default "";
    String type() default "";
    int length() default 0;
    boolean notNull() default false;
    String defaultValue() default "NULL";
}
