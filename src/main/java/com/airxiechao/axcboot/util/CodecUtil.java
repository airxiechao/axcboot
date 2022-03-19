package com.airxiechao.axcboot.util;

import java.util.Base64;

public class CodecUtil {

    public static String encodeBase64(String text){
        String encodeStr = Base64.getEncoder().encodeToString(text.getBytes());
        return encodeStr;
    }

}
