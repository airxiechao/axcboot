package com.airxiechao.axcboot.communication.rest.aspect;

import io.undertow.server.HttpServerExchange;

import java.util.Map;

public interface PinHandler {

    void handle(HttpServerExchange httpServerExchange, Map pinStore) throws Exception;

}
