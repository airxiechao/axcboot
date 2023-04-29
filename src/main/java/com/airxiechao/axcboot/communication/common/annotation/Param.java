package com.airxiechao.axcboot.communication.common.annotation;

import java.lang.annotation.*;

@Repeatable(Params.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Param {
    String value();
    boolean required();
}
