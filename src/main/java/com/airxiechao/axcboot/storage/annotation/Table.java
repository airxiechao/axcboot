package com.airxiechao.axcboot.storage.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {
    String value();
    String datasource() default "";
    String datasourceMethod() default "";
    String primaryKeyMethod() default "BTREE";
    boolean primaryKeyAutoIncrement() default true;
    int primaryKeyAutoIncrementBegin() default 1;
    String engine() default "InnoDB";
    String charset() default "utf8mb4";
    String collate() default "utf8mb4_general_ci";
    String rowFormat() default "Dynamic";
}
