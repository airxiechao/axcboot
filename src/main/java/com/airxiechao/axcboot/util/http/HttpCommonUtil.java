package com.airxiechao.axcboot.util.http;

import com.airxiechao.axcboot.communication.common.FileData;
import com.airxiechao.axcboot.util.StreamUtil;
import com.airxiechao.axcboot.util.StringUtil;
import com.alibaba.fastjson.JSON;
import okhttp3.*;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class HttpCommonUtil {
    private static final MediaType FORM_UTF8_CONTENT_TYPE = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
    private static final MediaType JSON_UTF8_CONTENT_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final String CHARSET_LATIN_1 = "iso-8859-1";

    public static String get(
            OkHttpClient client,
            String path,
            Map<String, Object> params,
            Map<String, String> headers,
            Map<String, String> cookies
    ) throws Exception {

        HttpUrl.Builder urlBuilder = HttpUrl.parse(path).newBuilder();

        if(null != params){
            params.forEach((name, value) -> {
                if(null != value){
                    if(value instanceof Date){
                        urlBuilder.addQueryParameter(name, String.valueOf(((Date)value).getTime()));
                    }else{
                        urlBuilder.addQueryParameter(name, value.toString());
                    }
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
            String ret = response.body().string();
            return ret;
        }
    }

    public static String postFormUrlEncoded(
            OkHttpClient client,
            String path,
            Map<String, Object> params,
            Map<String, String> headers,
            Map<String, String> cookies
    ) throws Exception {
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

    public static String postFormMultipart(
            OkHttpClient client,
            String path,
            Map<String, Object> params,
            Map<String, String> headers,
            Map<String, String> cookies,
            BiConsumer<Long, Long> totalAndSpeedConsumer,
            Supplier<Boolean> stopSupplier
    ) throws Exception {
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder();
        multipartBuilder.setType(MultipartBody.FORM);
        if(null != params) {
            params.forEach((name, value) -> {
                if (null != value) {
                    if(value instanceof File){
                        File file = (File)value;
                        String fileName = file.getName();
                        multipartBuilder.addFormDataPart(name, fileName,
                                new ProgressFileRequestBody(file, "application/octet-stream", totalAndSpeedConsumer, stopSupplier)
                        );
                    }else if(value instanceof FileData){
                        FileData fileData = (FileData)value;
                        File file = fileData.getFileItem().getFile().toFile();
                        String fileName = file.getName();
                        multipartBuilder.addFormDataPart(name, fileName,
                                new ProgressFileRequestBody(file, "application/octet-stream", totalAndSpeedConsumer, stopSupplier));
                    }else{
                        String val = value.toString();
                        multipartBuilder.addFormDataPart(name, val);
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
            OkHttpClient client,
            String path,
            Map<String, Object> params,
            Map<String, String> headers,
            Map<String, String> cookies
    ) throws Exception {
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
            OkHttpClient client,
            String path,
            Map<String, Object> params,
            Map<String, String> headers,
            Map<String, String> cookies,
            OutputStream outputStream,
            BiConsumer<Long, Long> totalAndSpeedConsumer,
            Supplier<Boolean> stopSupplier
    ) throws Exception {

        HttpUrl.Builder urlBuilder = HttpUrl.parse(path).newBuilder();

        if(null != params){
            params.forEach((name, value) -> {
                if(null != value){
                    if(value instanceof Date){
                        urlBuilder.addQueryParameter(name, String.valueOf(((Date)value).getTime()));
                    }else{
                        urlBuilder.addQueryParameter(name, value.toString());
                    }
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
            StreamUtil.readInputToOutputStream(inputStream, 1024, outputStream, totalAndSpeedConsumer, stopSupplier);
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
