package com.airxiechao.axcboot.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.util.Base64;

public class DesUtil {

    private static final Logger logger = LoggerFactory.getLogger(DesUtil.class);

    public static SecretKey buildDesKey(String key) throws Exception {
        byte keyBytes[] = key.getBytes();
        DESKeySpec desKeySpec = new DESKeySpec(keyBytes);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        return keyFactory.generateSecret(desKeySpec);
    }

    public static String encrypt(String key, String text) throws Exception {

        Cipher desCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        desCipher.init(Cipher.ENCRYPT_MODE, buildDesKey(key));

        return Base64.getEncoder().encodeToString(desCipher.doFinal(text.getBytes("UTF-8")));
    }

    public static String decrypt(String key, String text) throws Exception {

        Cipher desCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        desCipher.init(Cipher.DECRYPT_MODE, buildDesKey(key));

        return new String(desCipher.doFinal(Base64.getDecoder().decode(text)));
    }
}
