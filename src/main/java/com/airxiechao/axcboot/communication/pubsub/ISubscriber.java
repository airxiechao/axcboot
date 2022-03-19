package com.airxiechao.axcboot.communication.pubsub;

import com.airxiechao.axcboot.communication.common.Response;

import java.util.Map;

public interface ISubscriber {

    Response handle(Map<String, Object> params) throws Exception;
}
