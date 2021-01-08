package com.airxiechao.axcboot.communication.ltc.queue;

public interface ICallableEvent {

    Object handle() throws Exception;
}
