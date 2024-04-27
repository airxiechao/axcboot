package com.airxiechao.axcboot.communication.rest.annotation;

import com.airxiechao.axcboot.communication.rest.aspect.PinHandler;

import java.lang.annotation.*;

@Repeatable(Pins.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Pin {
    PinWhen when();
    Class<? extends PinHandler> handler();
}
