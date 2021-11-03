package com.airxiechao.axcboot.util;

import com.airxiechao.axcboot.communication.common.FileData;
import com.airxiechao.axcboot.crypto.SslUtil;
import com.alibaba.fastjson.JSON;
import okhttp3.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HttpsUtil {


    private static final MediaType FORM_UTF8_CONTENT_TYPE = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
    private static final MediaType JSON_UTF8_CONTENT_TYPE = MediaType.parse("application/json; charset=utf-8");

    public static String get(
            String path,
            Map<String, Object> params,
            Map<String, String> headers,
            Map<String, String> cookies,
            int timeout
    ) throws Exception {

        TrustManager trustAllManager = SslUtil.buildAllowAllTrustManager();
        final SSLContext sslContext = SslUtil.createSslContext(null, trustAllManager);
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllManager)
                .hostnameVerifier((hostname, session) -> true)
                .build();

        HttpUrl.Builder urlBuilder = HttpUrl.parse(path).newBuilder();
        params.forEach((name, value) -> {
            if(null != value){
                urlBuilder.addQueryParameter(name, value.toString());
            }
        });

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

    public static String postFormUrlEncoded(
            String path,
            Map<String, Object> params,
            Map<String, String> headers,
            Map<String, String> cookies,
            int timeout
    ) throws Exception {

        TrustManager trustAllManager = SslUtil.buildAllowAllTrustManager();
        final SSLContext sslContext = SslUtil.createSslContext(null, trustAllManager);
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllManager)
                .hostnameVerifier((hostname, session) -> true)
                .build();

        FormBody.Builder formBuilder = new FormBody.Builder();
        params.forEach((name, value) -> {
            if(null != value){
                formBuilder.add(name, value.toString());
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

    public static String postFormMultipart(
            String path,
            Map<String, Object> params,
            Map<String, String> headers,
            Map<String, String> cookies,
            int timeout) throws Exception {

        TrustManager trustAllManager = SslUtil.buildAllowAllTrustManager();
        final SSLContext sslContext = SslUtil.createSslContext(null, trustAllManager);
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllManager)
                .hostnameVerifier((hostname, session) -> true)
                .build();

        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder();
        multipartBuilder.setType(MultipartBody.FORM);
        if(null != params) {
            params.forEach((name, value) -> {
                if (null != value) {
                    if(value instanceof File){
                        File file = (File)value;
                        multipartBuilder.addFormDataPart(name, file.getName(),
                                RequestBody.create(file, MediaType.parse("application/octet-stream")));
                    }else if(value instanceof FileData){
                        FileData fileData = (FileData)value;
                        File file = fileData.getFileItem().getFile().toFile();
                        multipartBuilder.addFormDataPart(name, file.getName(),
                                RequestBody.create(file, MediaType.parse("application/octet-stream")));
                    }else{
                        multipartBuilder.addFormDataPart(name, value.toString());
                    }
                }
            });
        }

        RequestBody postBody = multipartBuilder.build();

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
            int timeout
    ) throws Exception {

        TrustManager trustAllManager = SslUtil.buildAllowAllTrustManager();
        final SSLContext sslContext = SslUtil.createSslContext(null, trustAllManager);
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllManager)
                .hostnameVerifier((hostname, session) -> true)
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

    public static boolean download(
            String path,
            Map<String, Object> params,
            Map<String, String> headers,
            Map<String, String> cookies,
            int timeout,
            OutputStream outputStream
    ) throws Exception {

        TrustManager trustAllManager = SslUtil.buildAllowAllTrustManager();
        final SSLContext sslContext = SslUtil.createSslContext(null, trustAllManager);
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllManager)
                .hostnameVerifier((hostname, session) -> true)
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

        addHeaderAndCookie(requestBuilder, headers, cookies);

        Request request = requestBuilder.build();

        try(Response response = client.newCall(request).execute()){
            if(!response.isSuccessful()){
                return false;
            }
            InputStream inputStream = response.body().byteStream();
            StreamUtil.readInputToOutputStream(inputStream, 1024, outputStream);
        }

        return true;
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
