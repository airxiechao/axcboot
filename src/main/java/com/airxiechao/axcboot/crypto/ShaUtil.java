package com.airxiechao.axcboot.crypto;

import org.apache.commons.codec.digest.DigestUtils;

public class ShaUtil {
    public static String sha1(String text){
        return DigestUtils.sha1Hex(text);
    }

    public static String sha256(String text){
        return DigestUtils.sha256Hex(text);
    }
}
