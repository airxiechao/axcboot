package com.airxiechao.axcboot.util;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class ConfigUtil {

    private static Config config = ConfigFactory.load("config.conf");

    public static Config getConfig(){
        return config;
    }
}
