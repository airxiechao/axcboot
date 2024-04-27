package com.airxiechao.axcboot.util;

import java.util.Arrays;
import java.util.stream.Collectors;

public class StringUtil {

    public static boolean isBlank(String str){
        if(null == str || str.isBlank()){
            return true;
        }

        return false;
    }

    public static boolean isEmpty(String str){
        if(null == str || str.isEmpty()){
            return true;
        }

        return false;
    }

    public static String notNull(String org){
        if(null == org){
            return "";
        }else{
            return org;
        }
    }

    public static String notNull(Long org){
        if(null == org){
            return "";
        }else{
            return org.toString();
        }
    }

    public static String notNull(Integer org){
        if(null == org){
            return "";
        }else{
            return org.toString();
        }
    }

    public static String notNull(Double org){
        if(null == org){
            return "";
        }else{
            return org.toString();
        }
    }

    public static String padding(int minLength, String text, char paddingChar){
        int len = text.length();
        if(len < minLength){
            int paddingLength = minLength - len;
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < paddingLength; ++i){
                sb.append(paddingChar);
            }
            sb.append(text);
            return sb.toString();
        }else{
            return text;
        }
    }

    public static String concat(String delimiter, String... items){
        return String.join(delimiter, Arrays.stream(items).filter(item -> !StringUtil.isBlank(item)).collect(Collectors.toList()));
    }

    public static String camelCaseToUnderscore(String str) {
        return str.replaceAll("[A-Z]", "_$0").toLowerCase();
    }
}
