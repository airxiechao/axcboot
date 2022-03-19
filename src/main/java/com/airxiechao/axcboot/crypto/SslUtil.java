package com.airxiechao.axcboot.crypto;

import com.airxiechao.axcboot.storage.fs.IFs;

import javax.net.ssl.*;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class SslUtil {

    public static KeyStore buildKeyStore(
            IFs keyStoreFs,
            String keyStoreFileName,
            String password
    ) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        char[] keyStorePassword = null;
        if(null != password){
            keyStorePassword = password.toCharArray();
        }

        try( InputStream keyStoreInputStream = keyStoreFs.getInputStream(keyStoreFileName) ){
            keyStore.load(keyStoreInputStream, keyStorePassword);
        }

        return keyStore;
    }

    public static KeyManagerFactory buildKeyManagerFactory(
            IFs keyStoreFs,
            String keyStoreFileName,
            String password
    ) throws Exception {
        KeyStore keyStore = buildKeyStore(keyStoreFs, keyStoreFileName, password);

        char[] keyStorePassword = null;
        if(null != password){
            keyStorePassword = password.toCharArray();
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, keyStorePassword);

        return keyManagerFactory;
    }

    public static TrustManagerFactory buildTrustManagerFactory(
            IFs keyStoreFs,
            String keyStoreFileName,
            String password
    ) throws Exception {
        KeyStore trustStore = buildKeyStore(keyStoreFs, keyStoreFileName, password);

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(trustStore);

        return trustManagerFactory;
    }

    public static TrustManager buildReloadableTrustManager(
            IFs keyStoreFs,
            String keyStoreFileName,
            String password
    ) throws Exception {
        return new ReloadableX509TrustManager(keyStoreFs, keyStoreFileName, password);
    }

    public static TrustManager buildAllowAllTrustManager(){
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }

    public static SSLContext createSslContext(KeyManagerFactory keyManagerFactory, TrustManager trustManager) throws Exception {
        KeyManager[] keyManagers = null == keyManagerFactory ? null : keyManagerFactory.getKeyManagers();
        TrustManager[] trustManagers = null == trustManager ? null : new TrustManager[]{ trustManager};

        SSLContext sslContext;
        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, null);

        return sslContext;
    }

}
