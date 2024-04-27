package com.airxiechao.axcboot.config.factory;

import com.airxiechao.axcboot.config.annotation.Config;
import com.airxiechao.axcboot.storage.fs.IFs;
import com.airxiechao.axcboot.storage.fs.JavaResourceFs;
import com.airxiechao.axcboot.util.AnnotationUtil;
import com.airxiechao.axcboot.util.StreamUtil;
import com.airxiechao.axcboot.util.StringUtil;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigFactory {

    private static ConfigFactory instance = new ConfigFactory();
    public static ConfigFactory getInstance(){
        return instance;
    }

    public static <T> T get(Class<T> cls){
        return getByFile(cls, new JavaResourceFs(), null);
    }

    public static <T> T getByFile(Class<T> cls, IFs configFileFs, String configFilePath){
        String configKey = buildConfigKey(cls, configFilePath);
        Object config = instance.configMap.get(configKey);
        if(null != config){
            return (T)config;
        }

        T loaded = instance.loadConfigFile(cls, configFileFs, configFilePath);
        instance.configMap.put(configKey, loaded);
        return loaded;
    }

    public static <T> T getByContent(Class<T> cls, String configName, String configContent){
        String configKey = buildConfigKey(cls, configName);
        Object config = instance.configMap.get(configKey);
        if(null != config){
            return (T)config;
        }

        T loaded = instance.loadConfigContent(cls, configContent);
        instance.configMap.put(configKey, loaded);
        return loaded;
    }

    private Map<String, Object> configMap = new ConcurrentHashMap<>();

    private ConfigFactory(){
    }

    private <T> T loadConfigFile(Class<T> cls, IFs ymlFs, String ymlPath){
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
        try(InputStream inputStream = ymlFs.getInputStream(ymlPath)) {
            T obj = yaml.loadAs(inputStream, cls);
            return obj;
        } catch (Exception e) {
            throw new RuntimeException("load " + ymlPath + " error");
        }
    }

    private <T> T loadConfigContent(Class<T> cls, String ymlContent){
        if(StringUtil.isBlank(ymlContent)){
            throw new RuntimeException("no yml content for " + cls.getSimpleName());
        }

        Representer representer = new Representer();
        representer.getPropertyUtils().setSkipMissingProperties(true);

        Yaml yaml = new Yaml(representer);
        try(InputStream inputStream = new ByteArrayInputStream(ymlContent.getBytes(StandardCharsets.UTF_8))) {
            T obj = yaml.loadAs(inputStream, cls);
            return obj;
        } catch (Exception e) {
            throw new RuntimeException("load yml config error");
        }
    }

    private static <T> String buildConfigKey(Class<T> cls, String ymlPath){
        return String.format("%s-%s", cls.getName(), ymlPath);
    }
}
