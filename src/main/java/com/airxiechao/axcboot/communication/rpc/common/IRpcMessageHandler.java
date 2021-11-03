package com.airxiechao.axcboot.communication.rpc.common;

import com.airxiechao.axcboot.communication.common.Response;

public interface IRpcMessageHandler {
    Response handle(RpcExchange exchange) throws Exception;
}
