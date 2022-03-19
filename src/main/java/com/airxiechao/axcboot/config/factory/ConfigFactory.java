package com.airxiechao.axcboot.config.factory;

import com.airxiechao.axcboot.config.annotation.Config;
import com.airxiechao.axcboot.storage.fs.IFs;
import com.airxiechao.axcboot.storage.fs.JavaResourceFs;
import com.airxiechao.axcboot.util.AnnotationUtil;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigFactory {

    private static ConfigFactory instance = new ConfigFactory();
    public static ConfigFactory getInstance(){
        return instance;
    }

    public static <T> T get(Class<T> cls){
        return get(cls, null);
    }

    public static <T> T get(Class<T> cls, String configFilePath){
        String configKey = buildConfigKey(cls, configFilePath);
        Object config = instance.configMap.get(configKey);
        if(null != config){
            return (T)config;
        }

        T loaded = instance.loadConfig(cls, configFilePath);
        instance.configMap.put(configKey, loaded);
        return loaded;
    }

    private IFs fs = new JavaResourceFs();
    private Map<String, Object> configMap = new ConcurrentHashMap<>();

    private ConfigFactory(){
    }

    private <T> T loadConfig(Class<T> cls, String ymlPath){
        if(null == ymlPath){
            Config config = AnnotationUtil.getClassAnnotation(cls, Config.class);
            if(null == config){
                throw new RuntimeException("no yml path for " + cls.getSimpleName());
            }
            ymlPath = config.value();
        }

        Representer representer = new Representer();
        representer.getPropertyUtils().setSkipMissingProperties(true);

        Yaml yaml = new Yaml(representer);
        try(InputStream inputStream = fs.getInputStream(ymlPath)) {
            T obj = yaml.loadAs(inputStream, cls);
            return obj;
        } catch (Exception e) {
            throw new RuntimeException("load " + ymlPath + " error");
        }
    }

    private static <T> String buildConfigKey(Class<T> cls, String ymlPath){
        return String.format("%s-%s", cls.getName(), ymlPath);
    }
}
