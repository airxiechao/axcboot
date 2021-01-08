package com.airxiechao.axcboot.communication.rpc.common;

public class RpcException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }

    public RpcException(String message) {
        super(message);
    }

    public RpcException(Throwable cause) {
        super(cause);
    }

}
