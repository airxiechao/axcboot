package com.airxiechao.axcboot.util.template;

import com.alibaba.fastjson.JSON;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.TemplateFunction;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.*;

public class StringTemplate {
    private static final Logger logger = LoggerFactory.getLogger(StringTemplate.class);
    private static DefaultMustacheFactory mf = new DefaultMustacheFactory();
    private static Map<String, Mustache> mustacheMap = new HashMap<>();

    static {
        // 在模板中引用数组的实现
        ReflectionObjectHandler oh = new ReflectionObjectHandler() {
            @Override
            public Object coerce(final Object object) {
                if (object != null && object.getClass().isArray()) {
                    return new ArrayMap(object);
                }else if (object != null && List.class.isAssignableFrom(object.getClass())) {
                    return new ArrayMap(((List)object).toArray());
                }else{
                    return super.coerce(object);
                }
            }
        };
        mf.setObjectHandler(oh);
    }

    private Mustache mustache;
    private String template;

    public StringTemplate(String template){
        if(null == template){
            template = "";
        }
        this.template = template;

        if(!mustacheMap.containsKey(template)){
            mustache = mf.compile(new StringReader(template), null);
            mustacheMap.put(template, mustache);
        }else{
            mustache = mustacheMap.get(template);
        }
    }

    public String render(Map<String, Object> scopes){
        Map<String, Object> copy = new HashMap();
        copy.putAll(scopes);

        // 添加自定义函数标签
        for (Class<? extends TemplateFunction> cls : StringTemplateFunctionFactory.getInstance().list()) {
            StringTemplateFunction anno = cls.getAnnotation(StringTemplateFunction.class);
            try {
                copy.put(anno.value(), cls.getDeclaredConstructor(Map.class).newInstance(copy));
            } catch (Exception e) {
                logger.error("add string template function {} error", cls.getName(), e);
            }
        }

        try( Writer writer = new StringWriter() ){
            mustache.execute(writer, copy);
            writer.flush();
            return writer.toString();
        }catch (Exception e){
            logger.error("render string template [{}] with [{}] error", template, JSON.toJSONString(copy), e);
            return "";
        }
    }

    private static class ArrayMap extends AbstractMap<Object, Object> implements Iterable<Object> {
        private final Object object;

        public ArrayMap(Object object) {
            this.object = object;
        }

        @Override
        public Object get(Object key) {
            try {
                int index = Integer.parseInt(key.toString());
                return Array.get(object, index);
            } catch (NumberFormatException nfe) {
                return null;
            }
        }

        @Override
        public boolean containsKey(Object key) {
            return get(key) != null;
        }

        @Override
        public Set<Entry<Object, Object>> entrySet() {
            throw new UnsupportedOperationException();
        }

        /**
         * Returns an iterator over a set of elements of type T.
         *
         * @return an Iterator.
         */
        @Override
        public Iterator<Object> iterator() {
            return new Iterator<Object>() {

                int index = 0;
                int length = Array.getLength(object);

                @Override
                public boolean hasNext() {
                    return index < length;
                }

                @Override
                public Object next() {
                    return Array.get(object, index++);
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }
}
