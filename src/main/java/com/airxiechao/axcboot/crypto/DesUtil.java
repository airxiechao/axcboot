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

    private static SecretKey desKey;

    static {
        try {
            byte key[] = "secretgarden".getBytes();
            DESKeySpec desKeySpec = new DESKeySpec(key);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            desKey = keyFactory.generateSecret(desKeySpec);
        } catch (Exception e) {
            logger.error("des key generate error", e);
        }
    }

    public static String encrypt(String text) throws Exception {

        Cipher desCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        desCipher.init(Cipher.ENCRYPT_MODE, desKey);

        return Base64.getEncoder().encodeToString(desCipher.doFinal(text.getBytes("UTF-8")));
    }

    public static String decrpty(String text) throws Exception {

        Cipher desCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        desCipher.init(Cipher.DECRYPT_MODE, desKey);

        return new String(desCipher.doFinal(Base64.getDecoder().decode(text)));
    }
}
