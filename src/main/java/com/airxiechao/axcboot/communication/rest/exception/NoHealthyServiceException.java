package com.airxiechao.axcboot.communication.rest.exception;

public class NoHealthyServiceException extends Exception {
    public NoHealthyServiceException() {
    }

    public NoHealthyServiceException(String message) {
        super(message);
    }

    public NoHealthyServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoHealthyServiceException(Throwable cause) {
        super(cause);
    }

    public NoHealthyServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
