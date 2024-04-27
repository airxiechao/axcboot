package com.airxiechao.axcboot.util.template;

import com.airxiechao.axcboot.util.ClsUtil;
import com.github.mustachejava.TemplateFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class StringTemplateFunctionFactory {
    private static final Logger logger = LoggerFactory.getLogger(StringTemplateFunctionFactory.class);
    private static StringTemplateFunctionFactory instance = new StringTemplateFunctionFactory();
    public static StringTemplateFunctionFactory getInstance(){
        return instance;
    }

    private List<Class<? extends TemplateFunction>> functions = new ArrayList<>();

    public StringTemplateFunctionFactory() {
        String pkg = this.getClass().getPackage().getName();

        // 扫描标签函数
        Set<Class<?>> classes = ClsUtil.getTypesAnnotatedWith(pkg, StringTemplateFunction.class);
        for (Class<?> cls : classes) {
            if(TemplateFunction.class.isAssignableFrom(cls)){
                logger.info("register string template function [{}]", cls.getName());
                functions.add((Class<? extends TemplateFunction>)cls);
            }
        }
    }

    public List<Class<? extends TemplateFunction>> list(){
        return functions;
    }
}
