package com.airxiechao.axcboot.communication.pubsub;

import com.airxiechao.axcboot.communication.common.Response;

import java.util.Map;

public interface IPubSub {

    Response publish(String event, Map<String, Object> params);
    void subscribe(String event, String subscriber, ISubscriber handler);
    void unsubscribe(String event, String subscriber);
    void registerSink(Class<? extends ISubscriber> cls);
    void unregisterSink(Class<? extends ISubscriber> cls);
    void clearSubscriber(String event);
    void clearSubscriber();
}
