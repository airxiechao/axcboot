package com.airxiechao.axcboot.util;

import java.nio.charset.Charset;
public class CharsetUtil {

    public static Charset fromSystem(){
        String os = System.getProperty("os.name").toLowerCase();
        String lang = System.getProperty("user.language").toLowerCase();

        if(os.startsWith("win") && lang.equals("zh")){
            return Charset.forName("GBK");
        }else{
            return Charset.forName("UTF-8");
        }
    }
}
