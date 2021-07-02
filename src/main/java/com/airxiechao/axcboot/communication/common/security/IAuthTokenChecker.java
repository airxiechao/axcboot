package com.airxiechao.axcboot.communication.common.security;

public interface IAuthTokenChecker {
    boolean validate(String authToken, String scope, String item, int mode);

}
