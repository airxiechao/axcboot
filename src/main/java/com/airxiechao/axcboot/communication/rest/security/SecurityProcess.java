package com.airxiechao.axcboot.communication.rest.security;

import com.airxiechao.axcboot.crypto.DesUtil;
import com.alibaba.fastjson.JSON;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.CookieImpl;

public class SecurityProcess {

    public static AuthPrincipal getAuthPrincipalFromToken(String token){
        try{
            String json = DesUtil.decrpty(token);
            AuthPrincipal authUser = JSON.parseObject(json, AuthPrincipal.class);
            return authUser;
        }catch (Exception e){
            return null;
        }
    }

    public static String genAuthToken(AuthPrincipal authPrincipal) throws Exception {
        String json = JSON.toJSONString(authPrincipal);
        return DesUtil.encrypt(json);
    }


    public static void setAuthTokenCookie(HttpServerExchange httpServerExchange, String authToken){
        Cookie authCookie = new CookieImpl("auth", authToken);
        authCookie.setPath("/");
        httpServerExchange.setResponseCookie(authCookie);
    }

    public static GuardPrincipal getGuardPrincipalFromToken(String token){
        try{
            String json = DesUtil.decrpty(token);
            GuardPrincipal guardPrincipal = JSON.parseObject(json, GuardPrincipal.class);
            return guardPrincipal;
        }catch (Exception e){
            return null;
        }
    }

    public static String genGuardToken(GuardPrincipal guardPrincipal) throws Exception {
        String json = JSON.toJSONString(guardPrincipal);
        return DesUtil.encrypt(json);
    }
}
