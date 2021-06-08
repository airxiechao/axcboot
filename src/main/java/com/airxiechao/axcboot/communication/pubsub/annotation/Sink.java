package com.airxiechao.axcboot.communication.pubsub.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Sink {
    String[] events();
    String name() default "";
}
