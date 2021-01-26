package com.airxiechao.axcboot.crypto;

import com.airxiechao.axcboot.storage.fs.IFs;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class SslUtil {

    public static KeyManagerFactory buildKeyManagerFactory(
            IFs keyStoreFs,
            String keyStoreFileName,
            String password
    ) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        char[] keyStorePassword = null;
        if(null != password){
            keyStorePassword = password.toCharArray();
        }

        try( InputStream keyStoreInputStream = keyStoreFs.getFileAsStream(keyStoreFileName) ){
            keyStore.load(keyStoreInputStream, keyStorePassword);
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, keyStorePassword);

        return keyManagerFactory;
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
}
