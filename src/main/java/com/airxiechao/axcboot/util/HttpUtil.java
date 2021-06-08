package com.airxiechao.axcboot.util;

import com.alibaba.fastjson.JSON;
import okhttp3.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HttpUtil {


    private static final MediaType FORM_UTF8_CONTENT_TYPE = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
    private static final MediaType JSON_UTF8_CONTENT_TYPE = MediaType.parse("application/json; charset=utf-8");

    public static String get(String path, Map<String, String> params, int timeout) throws Exception {

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .build();

        HttpUrl.Builder urlBuilder = HttpUrl.parse(path).newBuilder();
        params.forEach((name, value) -> {
            if(null != value){
                urlBuilder.addQueryParameter(name, value);
            }
        });

        String url = urlBuilder.build().toString();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try(Response response = client.newCall(request).execute()){
            String ret = response.body().string();
            return ret;
        }
    }

    public static String postForm(String path, Map<String, String> params, int timeout) throws Exception {

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .build();

        FormBody.Builder formBuilder = new FormBody.Builder();
        params.forEach((name, value) -> {
            if(null != value){
                formBuilder.add(name, value);
            }
        });
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

        Request request = new Request.Builder()
                .url(path)
                .post(postBody)
                .build();

        try(Response response = client.newCall(request).execute()){
            String ret = response.body().string();
            return ret;
        }
    }

    public static String postJson(String path, Map<String, String> params, int timeout) throws Exception {

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .build();

        String body = JSON.toJSONString(params);
        RequestBody postBody = RequestBody.create(body, JSON_UTF8_CONTENT_TYPE);

        Request request = new Request.Builder()
                .url(path)
                .post(postBody)
                .build();

        try(Response response = client.newCall(request).execute()){
            String ret = response.body().string();
            return ret;
        }
    }
}
