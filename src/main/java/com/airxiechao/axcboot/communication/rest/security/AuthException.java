package com.airxiechao.axcboot.communication.rest.security;

public class AuthException extends Exception {

    public AuthException(){
        super();
    }

    public AuthException(String message){
        super(message);
    }
}
