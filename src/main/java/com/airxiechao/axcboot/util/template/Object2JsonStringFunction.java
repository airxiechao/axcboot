package com.airxiechao.axcboot.util.template;

import com.alibaba.fastjson.JSON;
import com.github.mustachejava.TemplateFunction;

import java.util.Map;

/**
 * 对象转字符串的标签函数。
 *
 * 使用方法：
 * {{#object2jsonstring}}字段名 [是否格式化]{{/object2jsonstring}}
 *
 * 比如：
 * {{#object2jsonstring}}value{{/object2jsonstring}} 或
 * {{#object2jsonstring}}value true{{/object2jsonstring}}
 */
@StringTemplateFunction("object2jsonstring")
public class Object2JsonStringFunction implements TemplateFunction {
    private Map<String, Object> scopes;

    public Object2JsonStringFunction(Map<String, Object> scopes) {
        this.scopes = scopes;
    }

    @Override
    public String apply(String s) {
        s = s.trim();

        String field = s;
        boolean pretty = false;

        int sep = s.indexOf(" ");
        if(sep > 0){
            field = s.substring(0, sep);
            pretty = Boolean.valueOf(s.substring(sep + 1).trim());
        }

        Object value = scopes.get(field);
        if(null == value){
            return "";
        }

        return JSON.toJSONString(value, pretty);
    }
}
