package com.airxiechao.axcboot.crypto;

import com.airxiechao.axcboot.storage.fs.IFs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

class ReloadableX509TrustManager implements X509TrustManager {

    private static final Logger logger = LoggerFactory.getLogger(ReloadableX509TrustManager.class);

    private IFs keyStoreFs;
    private String keyStoreFileName;
    private char[] keyStorePassword = null;
    private X509TrustManager trustManager;
    private List tempCertList= new ArrayList();

    public ReloadableX509TrustManager(IFs keyStoreFs, String keyStoreFileName, String password) throws Exception {
        this.keyStoreFs = keyStoreFs;
        this.keyStoreFileName = keyStoreFileName;
        if(null != password){
            this.keyStorePassword = password.toCharArray();
        }
        reloadTrustManager();
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        try {
            reloadTrustManager();
        } catch (Exception e) {
            logger.error("reload trust store [{}] error", this.keyStoreFileName, e);
        }
        this.trustManager.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        try {
            reloadTrustManager();
        } catch (Exception e) {
            logger.error("reload trust store [{}] error", this.keyStoreFileName, e);
        }
        this.trustManager.checkServerTrusted(chain, authType);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        X509Certificate[] issuers = this.trustManager.getAcceptedIssuers();
        return issuers;
    }

    private void reloadTrustManager() throws Exception {

        // load keystore
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try( InputStream keyStoreInputStream = this.keyStoreFs.getInputStream(this.keyStoreFileName) ) {
            keyStore.load(keyStoreInputStream, this.keyStorePassword);
        }

        // initialize a new TMF with the keystore we just loaded
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(keyStore);

        // acquire X509 trust manager from factory
        TrustManager tms[] = tmf.getTrustManagers();
        for (int i = 0; i < tms.length; ++i) {
            if (tms[i] instanceof X509TrustManager) {
                this.trustManager = (X509TrustManager)tms[i];
                return;
            }
        }
    }
}