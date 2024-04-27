package com.airxiechao.axcboot.crypto;

import org.apache.commons.codec.digest.DigestUtils;

public class Md5Util {

    public static String md5(String text){
        return DigestUtils.md5Hex(text);
    }

}
