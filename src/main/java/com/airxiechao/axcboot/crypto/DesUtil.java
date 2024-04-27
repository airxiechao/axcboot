package com.airxiechao.axcboot.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.Base64;

public class DesUtil {

    private static final Logger logger = LoggerFactory.getLogger(DesUtil.class);

    public static SecretKey buildDesKey(String key) throws Exception {
        byte keyBytes[] = key.getBytes(StandardCharsets.UTF_8);
        DESKeySpec desKeySpec = new DESKeySpec(keyBytes);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        return keyFactory.generateSecret(desKeySpec);
    }

    public static SecretKey buildDesKeyByPBKDF2(String key, String salt) throws Exception {
        KeySpec spec = new PBEKeySpec(key.toCharArray(), salt.getBytes(StandardCharsets.UTF_8), 1000, 64);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "DES");
        return secret;
    }

    public static String encrypt(String key, String text) throws Exception {

        Cipher desCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        desCipher.init(Cipher.ENCRYPT_MODE, buildDesKey(key));

        return Base64.getUrlEncoder().encodeToString(desCipher.doFinal(text.getBytes("UTF-8")));
    }

    public static String encryptByPBKDF2(String key, String salt, String text) throws Exception {

        Cipher desCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        desCipher.init(Cipher.ENCRYPT_MODE, buildDesKeyByPBKDF2(key, salt));

        return Base64.getUrlEncoder().encodeToString(desCipher.doFinal(text.getBytes("UTF-8")));
    }

    public static String decrypt(String key, String text) throws Exception {

        Cipher desCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        desCipher.init(Cipher.DECRYPT_MODE, buildDesKey(key));

        return new String(desCipher.doFinal(Base64.getUrlDecoder().decode(text)));
    }

    public static String decryptByPBKDF2(String key, String salt, String text) throws Exception {

        Cipher desCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        desCipher.init(Cipher.DECRYPT_MODE, buildDesKeyByPBKDF2(key, salt));

        return new String(desCipher.doFinal(Base64.getUrlDecoder().decode(text)));
    }
}
