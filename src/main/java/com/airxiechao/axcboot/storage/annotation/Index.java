package com.airxiechao.axcboot.storage.annotation;

import java.lang.annotation.*;

@Repeatable(Indexes.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Index {
    String[] fields();
    boolean unique() default false;
    String method() default "BTREE";
}
