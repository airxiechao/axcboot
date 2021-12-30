package com.airxiechao.axcboot.util;

import com.airxiechao.axcboot.crypto.SslUtil;
import com.airxiechao.axcboot.util.http.HttpCommonUtil;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class HttpUtil {

    private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);

    private static OkHttpClient buildClient(int timeout){
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .build();

        return client;
    }

    private static OkHttpClient buildSslClient(int timeout) throws Exception {
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

        return client;
    }

    public static String get(
            String path,
            Map<String, Object> params,
            Map<String, String> headers,
            Map<String, String> cookies,
            int timeout,
            boolean useSsl
    ) throws Exception {
        OkHttpClient client = useSsl ? buildSslClient(timeout) : buildClient(timeout);
        return HttpCommonUtil.get(client, path, params, headers, cookies);
    }

    public static String postFormUrlEncoded(
            String path,
            Map<String, Object> params,
            Map<String, String> headers,
            Map<String, String> cookies,
            int timeout,
            boolean useSsl
    ) throws Exception {
        OkHttpClient client = useSsl ? buildSslClient(timeout) : buildClient(timeout);
        return HttpCommonUtil.postFormUrlEncoded(client, path, params, headers, cookies);
    }

    public static String postFormMultipart(
            String path,
            Map<String, Object> params,
            Map<String, String> headers,
            Map<String, String> cookies,
            int timeout,
            boolean useSsl,
            BiConsumer<Long, Long> totalAndSpeedConsumer,
            Supplier<Boolean> stopSupplier
    ) throws Exception {
        OkHttpClient client = useSsl ? buildSslClient(timeout) : buildClient(timeout);
        return HttpCommonUtil.postFormMultipart(client, path, params, headers, cookies, totalAndSpeedConsumer, stopSupplier);
    }

    public static String postJson(
            String path,
            Map<String, Object> params,
            Map<String, String> headers,
            Map<String, String> cookies,
            int timeout,
            boolean useSsl
    ) throws Exception {
        OkHttpClient client = useSsl ? buildSslClient(timeout) : buildClient(timeout);
        return HttpCommonUtil.postJson(client, path, params, headers, cookies);
    }

    public static boolean download(
            String path,
            Map<String, Object> params,
            Map<String, String> headers,
            Map<String, String> cookies,
            int timeout,
            boolean useSsl,
            OutputStream outputStream,
            BiConsumer<Long, Long> totalAndSpeedConsumer,
            Supplier<Boolean> stopSupplier
    ) throws Exception {
        OkHttpClient client = useSsl ? buildSslClient(timeout) : buildClient(timeout);
        return HttpCommonUtil.download(client, path, params, headers, cookies, outputStream, totalAndSpeedConsumer, stopSupplier);
    }

}
