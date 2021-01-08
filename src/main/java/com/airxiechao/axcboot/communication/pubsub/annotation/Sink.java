package com.airxiechao.axcboot.communication.pubsub.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Sink {
    String[] events() default  {};
    String name() default "";
}
