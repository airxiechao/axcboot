package com.airxiechao.axcboot.util;

import com.alibaba.fastjson.JSON;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HttpUtil {

    private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);

    private static final MediaType FORM_UTF8_CONTENT_TYPE = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
    private static final MediaType JSON_UTF8_CONTENT_TYPE = MediaType.parse("application/json; charset=utf-8");

    public static String get(
            String path,
            Map<String, Object> params,
            Map<String, String> headers,
            Map<String, String> cookies,
            int timeout) throws Exception {

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .build();

        HttpUrl.Builder urlBuilder = HttpUrl.parse(path).newBuilder();

        if(null != params){
            params.forEach((name, value) -> {
                if(null != value){
                    urlBuilder.addQueryParameter(name, value.toString());
                }
            });
        }

        String url = urlBuilder.build().toString();
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .get();

        Request request = requestBuilder.build();

        addHeaderAndCookie(requestBuilder, headers, cookies);

        try(Response response = client.newCall(request).execute()){
            String ret = response.body().string();
            return ret;
        }
    }

    public static String postForm(
            String path,
            Map<String, Object> params,
            Map<String, String> headers,
            Map<String, String> cookies,
            int timeout) throws Exception {

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .build();

        FormBody.Builder formBuilder = new FormBody.Builder();
        if(null != params) {
            params.forEach((name, value) -> {
                if (null != value) {
                    formBuilder.add(name, value.toString());
                }
            });
        }
        FormBody formBody = formBuilder.build();

        // encode to utf-8
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < formBody.size(); ++i){
            if(i == 0){
                sb.append(formBody.encodedName(i)+"="+formBody.encodedValue(i));
            }else{
                sb.append("&"+formBody.encodedName(i)+"="+formBody.encodedValue(i));
            }
        }
        String body = sb.toString();
        RequestBody postBody = RequestBody.create(body, FORM_UTF8_CONTENT_TYPE);

        Request.Builder requestBuilder = new Request.Builder()
                .url(path)
                .post(postBody);

        addHeaderAndCookie(requestBuilder, headers, cookies);

        Request request = requestBuilder.build();

        try(Response response = client.newCall(request).execute()){
            String ret = response.body().string();
            return ret;
        }
    }

    public static String postJson(
            String path,
            Map<String, Object> params,
            Map<String, String> headers,
            Map<String, String> cookies,
            int timeout) throws Exception {

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .build();

        String body = JSON.toJSONString(params);
        RequestBody postBody = RequestBody.create(body, JSON_UTF8_CONTENT_TYPE);

        Request.Builder requestBuilder = new Request.Builder()
                .url(path)
                .post(postBody);

        addHeaderAndCookie(requestBuilder, headers, cookies);

        Request request = requestBuilder.build();

        try(Response response = client.newCall(request).execute()){
            String ret = response.body().string();
            return ret;
        }
    }

    private static void addHeaderAndCookie(
            Request.Builder requestBuilder,
            Map<String, String> headers,
            Map<String, String> cookies
    ){
        if(null != headers){
            headers.forEach((name, value) -> {
                if(null != value){
                    requestBuilder.addHeader(name, value);
                }
            });
        }

        if(null != cookies){
            String cookieHeader = cookies.entrySet().stream()
                    .map(entry -> {
                        return entry.getKey() + "=" + entry.getValue();
                    })
                    .collect(Collectors.joining("; "));

            requestBuilder.addHeader("Cookie", cookieHeader);
        }
    }
}
