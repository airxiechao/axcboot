package com.airxiechao.axcboot.communication.rest.util;

import com.airxiechao.axcboot.communication.common.PageParam;
import com.airxiechao.axcboot.communication.rest.annotation.Delete;
import com.airxiechao.axcboot.communication.rest.annotation.Get;
import com.airxiechao.axcboot.communication.rest.annotation.Post;
import com.airxiechao.axcboot.communication.rest.security.AuthPrincipal;
import com.airxiechao.axcboot.communication.rest.security.GuardPrincipal;
import com.airxiechao.axcboot.communication.rest.security.SecurityProcess;
import com.airxiechao.axcboot.util.TimeUtil;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.*;

public class RestUtil {

    public static Map<String, String>  allQueryStringParam(HttpServerExchange exchange){
        Map<String, String> map = new HashMap<>();

        exchange.getQueryParameters().keySet().forEach(name -> {
            Deque<String> param = exchange.getQueryParameters().get(name);
            if(null != param && param.size() > 0 && !param.getFirst().isBlank()){
                map.put(name, param.getFirst().trim());
            }
        });

        return map;
    }

    public static String queryWsStringParam(WebSocketHttpExchange exchange, String name){
        List<String> param = exchange.getRequestParameters().get(name);
        if(null == param || param.get(0).isBlank()){
            return null;
        }else{
            return param.get(0).trim();
        }
    }

    public static String queryStringParam(HttpServerExchange exchange, String name){
        Deque<String> param = exchange.getQueryParameters().get(name);
        if(null == param || param.getFirst().isBlank()){
            return null;
        }else{
            return param.getFirst().trim();
        }
    }

    public static Integer queryIntegerParam(HttpServerExchange exchange, String name){
        String param = queryStringParam(exchange, name);
        if(null == param || param.isBlank()){
            return null;
        }else{
            return Integer.parseInt(param);
        }
    }

    public static Double queryDoubleParam(HttpServerExchange exchange, String name){
        String param = queryStringParam(exchange, name);
        if(null == param || param.isBlank()){
            return null;
        }else{
            return Double.parseDouble(param);
        }
    }

    public static Long queryLongParam(HttpServerExchange exchange, String name){
        String param = queryStringParam(exchange, name);
        if(null == param || param.isBlank()){
            return null;
        }else{
            return Long.parseLong(param);
        }
    }

    public static Date queryTimeParam(HttpServerExchange exchange, String name){
        try {
            return TimeUtil.toTime(queryStringParam(exchange, name));
        } catch (ParseException e) {
            return null;
        }
    }

    public static PageParam queryPageParam(HttpServerExchange exchange){
        Integer pageNo = RestUtil.queryIntegerParam(exchange, "pageNo");
        Integer pageSize = RestUtil.queryIntegerParam(exchange, "pageSize");
        String orderField = RestUtil.queryStringParam(exchange, "orderField");
        String orderType = RestUtil.queryStringParam(exchange, "orderType");

        PageParam pageParam = new PageParam(pageNo, pageSize, orderField, orderType);

        return pageParam;
    }

    public static Map<String, String> allFormStringData(HttpServerExchange exchange){
        Map<String, String> map = new HashMap<>();

        FormData formData = exchange.getAttachment(FormDataParser.FORM_DATA);
        if(null != formData){
            for(String name : formData){
                if(null != formData.get(name) &&
                        null != formData.get(name).getFirst() &&
                        null != formData.get(name).getFirst().getValue() &&
                        !formData.get(name).getFirst().getValue().isBlank()){
                    map.put(name, formData.get(name).getFirst().getValue().trim());
                }
            }
        }

        return map;
    }

    public static String formStringData(HttpServerExchange exchange, String name){
        FormData formData = exchange.getAttachment(FormDataParser.FORM_DATA);

        if(null != formData && null != formData.get(name) &&
                null != formData.get(name).getFirst() && null != formData.get(name).getFirst().getValue() &&
                !formData.get(name).getFirst().getValue().isBlank()){
            return formData.get(name).getFirst().getValue().trim();
        }else{
            return null;
        }
    }

    public static Integer formIntegerData(HttpServerExchange exchange, String name){
        String param = formStringData(exchange, name);
        if(null == param || param.isBlank()){
            return null;
        }else{
            return Integer.parseInt(param);
        }
    }

    public static Long formLongData(HttpServerExchange exchange, String name){
        String param = formStringData(exchange, name);
        if(null == param || param.isBlank()){
            return null;
        }else{
            return Long.parseLong(param);
        }
    }

    public static Double formDoubleData(HttpServerExchange exchange, String name){
        String param = formStringData(exchange, name);
        if(null == param || param.isBlank()){
            return null;
        }else{
            return Double.parseDouble(param);
        }
    }

    public static Date formTimeData(HttpServerExchange exchange, String name){
        try {
            return TimeUtil.toTime(formStringData(exchange, name));
        } catch (ParseException e) {
            return null;
        }
    }

    public static Path formFileData(HttpServerExchange exchange, String name){
        FormData formData = exchange.getAttachment(FormDataParser.FORM_DATA);

        if(null != formData  && null != formData.get(name) &&
                null != formData.get(name).getFirst() && formData.get(name).getFirst().isFileItem() ){
            return formData.get(name).getFirst().getFileItem().getFile();
        }else{
            return null;
        }
    }

    public static void redirect(HttpServerExchange httpServerExchange, String path){
        httpServerExchange.setStatusCode(StatusCodes.FOUND);
        httpServerExchange.getResponseHeaders().put(Headers.LOCATION, path);
        httpServerExchange.endExchange();
    }

    public static String getRestPath(Method method){
        String path = "";

        Get get = method.getAnnotation(Get.class);
        if(null != get){
            path = get.value();
        }

        Post post = method.getAnnotation(Post.class);
        if(null != post){
            path = post.value();
        }

        Delete delete = method.getAnnotation(Delete.class);
        if(null != delete) {
            path = delete.value();
        }

        return "/api/"+path;
    }

    public static String getRemoteIp(HttpServerExchange exchange){
        HeaderValues xff = exchange.getRequestHeaders().get("X-Forwarded-For");
        if(null != xff && xff.size() > 0){
            return xff.getFirst();
        }else{
            return exchange.getSourceAddress().getAddress().getHostAddress();
        }
    }

    public static String getAuthToken(HttpServerExchange exchange){
        String token = null;

        Cookie authCookie = exchange.getRequestCookies().get("auth");
        if(null == authCookie){
            token = RestUtil.queryStringParam(exchange, "auth");
            if(null == token){
                token = RestUtil.formStringData(exchange, "auth");
            }
        }else{
            token = authCookie.getValue();
        }

        return token;
    }

    public static String getWsAuthToken(WebSocketHttpExchange exchange){
        String token = RestUtil.queryWsStringParam(exchange, "auth");
        return token;
    }

    public static AuthPrincipal getWsAuthPrincipal(WebSocketHttpExchange exchange){
        String token = RestUtil.getWsAuthToken(exchange);

        if(null == token || token.isBlank()){
            return null;
        }

        AuthPrincipal authPrincipal = SecurityProcess.getAuthPrincipalFromToken(token);
        return authPrincipal;
    }

    public static AuthPrincipal getAuthPrincipal(HttpServerExchange exchange){
        String token = RestUtil.getAuthToken(exchange);

        if(null == token || token.isBlank()){
            return null;
        }

        AuthPrincipal authPrincipal = SecurityProcess.getAuthPrincipalFromToken(token);
        return authPrincipal;
    }

    public static String getGuardToken(HttpServerExchange exchange){
        String token = RestUtil.queryStringParam(exchange, "guard");
        if(null == token){
            token = RestUtil.formStringData(exchange, "guard");
        }

        return token;
    }

    public static GuardPrincipal getGuardPrincipal(HttpServerExchange exchange){
        String token = RestUtil.getGuardToken(exchange);

        if(null == token || token.isBlank()){
            return null;
        }

        GuardPrincipal guardPrincipal = SecurityProcess.getGuardPrincipalFromToken(token);
        return guardPrincipal;
    }

    public static String header(HttpServerExchange exchange, String name){
        HeaderValues headerValues = exchange.getRequestHeaders().get(name);
        if(null != headerValues && headerValues.size() > 0){
            return headerValues.getFirst();
        }

        return null;
    }

}

