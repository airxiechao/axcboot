package com.airxiechao.axcboot.storage.cache.persistence;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

import java.util.Map;

public class PersistenceCache<K> {

    private MVStore store;
    private MVMap<K, String> map;

    public PersistenceCache(String fileName){
        this.store =  new MVStore.Builder()
                .fileName(fileName)
                .open();
        this.map = this.store.openMap("data");
    }

    public MVStore store(){
        return this.store;
    }

    public Map<K, String> map(){
        return this.map;
    }

    public void commit(){
        this.store.commit();
    }

    public void close(){
        this.store.close();
    }
}
