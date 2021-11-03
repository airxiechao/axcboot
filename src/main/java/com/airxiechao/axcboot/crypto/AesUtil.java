package com.airxiechao.axcboot.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.Base64;

public class AesUtil {

    private static final Logger logger = LoggerFactory.getLogger(AesUtil.class);

    public static SecretKey buildAesKeyByPBKDF2(String key, String salt) throws Exception {
        KeySpec spec = new PBEKeySpec(key.toCharArray(), salt.getBytes(StandardCharsets.UTF_8), 1000, 256);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        return secret;
    }

    public static String encryptByPBKDF2(String key, String salt, String text) throws Exception {

        Cipher aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        aesCipher.init(Cipher.ENCRYPT_MODE, buildAesKeyByPBKDF2(key, salt));

        return Base64.getUrlEncoder().encodeToString(aesCipher.doFinal(text.getBytes("UTF-8")));
    }

    public static String decryptByPBKDF2(String key, String salt, String text) throws Exception {

        Cipher aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        aesCipher.init(Cipher.DECRYPT_MODE, buildAesKeyByPBKDF2(key, salt));

        return new String(aesCipher.doFinal(Base64.getUrlDecoder().decode(text)));
    }
}
