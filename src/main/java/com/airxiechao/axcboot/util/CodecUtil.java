package com.airxiechao.axcboot.util;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.Base64;

public class CodecUtil {

    public static String encodeBase64(String text){
        String encodeStr = Base64.getEncoder().encodeToString(text.getBytes());
        return encodeStr;
    }

    public static String md5(String text){
        return DigestUtils.md5Hex(text);
    }
}
