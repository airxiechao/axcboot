package com.airxiechao.axcboot.util.template;

import com.github.mustachejava.TemplateFunction;

import java.text.SimpleDateFormat;
import java.util.Map;

/**
 * 时间戳转日期格式字符串的标签函数。
 *
 * 使用方法：
 * {{#timestamp2string}}字段名 [时间格式]{{/timestamp2string}}
 *
 * 比如：
 * {{#timestamp2string}}logTime{{/timestamp2string}} 或
 * {{#timestamp2string}}logTime yyyy-MM-dd{{/timestamp2string}}
 */
@StringTemplateFunction("timestamp2string")
public class Timestamp2StringFunction implements TemplateFunction {
    private Map<String, Object> scopes;

    public Timestamp2StringFunction(Map<String, Object> scopes) {
        this.scopes = scopes;
    }

    @Override
    public String apply(String s) {
        s = s.trim();

        String field = s;
        String format = "yyyy-MM-dd HH:mm:ss";

        int sep = s.indexOf(" ");
        if(sep > 0){
            field = s.substring(0, sep);
            format = s.substring(sep + 1).trim();
        }

        Object value = scopes.get(field);
        if(null == value){
            return "";
        }

        return new SimpleDateFormat(format).format(value);
    }
}
