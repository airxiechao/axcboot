package com.airxiechao.axcboot.communication.rest.security;

import com.airxiechao.axcboot.crypto.DesUtil;
import com.alibaba.fastjson.JSON;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.CookieImpl;

public class SecurityProcess {

    public static AuthPrincipal getAuthPrincipalFromToken(String key, String token){
        try{
            String json = DesUtil.decrypt(key, token);
            AuthPrincipal authUser = JSON.parseObject(json, AuthPrincipal.class);
            return authUser;
        }catch (Exception e){
            return null;
        }
    }

    public static String genAuthToken(String key, AuthPrincipal authPrincipal) throws Exception {
        String json = JSON.toJSONString(authPrincipal);
        return DesUtil.encrypt(key, json);
    }


    public static void setAuthTokenCookie(HttpServerExchange httpServerExchange, String authToken){
        Cookie authCookie = new CookieImpl("auth", authToken);
        authCookie.setPath("/");
        httpServerExchange.setResponseCookie(authCookie);
    }

    public static GuardPrincipal getGuardPrincipalFromToken(String key, String token){
        try{
            String json = DesUtil.decrypt(key, token);
            GuardPrincipal guardPrincipal = JSON.parseObject(json, GuardPrincipal.class);
            return guardPrincipal;
        }catch (Exception e){
            return null;
        }
    }

    public static String genGuardToken(String key, GuardPrincipal guardPrincipal) throws Exception {
        String json = JSON.toJSONString(guardPrincipal);
        return DesUtil.encrypt(key, json);
    }
}
