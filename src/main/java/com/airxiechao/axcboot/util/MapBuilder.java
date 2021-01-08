package com.airxiechao.axcboot.util;

import java.util.HashMap;
import java.util.Map;

public class MapBuilder<K, V> {

    private Map<K, V> map;

    public MapBuilder(){
        this.map = new HashMap<>();
    }

    public MapBuilder<K, V> put(K key, V value){
        this.map.put(key, value);
        return this;
    }

    public Map<K, V> build(){
        return this.map;
    }

}
