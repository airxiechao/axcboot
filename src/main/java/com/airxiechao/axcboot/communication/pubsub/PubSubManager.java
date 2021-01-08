package com.airxiechao.axcboot.communication.pubsub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PubSubManager {

    private static final Logger logger = LoggerFactory.getLogger(PubSubManager.class);

    private static PubSubManager ourInstance = new PubSubManager();

    private Map<String, PubSubWorker> workers = new ConcurrentHashMap<>();

    public static PubSubManager getInstance() {
        return ourInstance;
    }

    private PubSubManager() {
    }

    public PubSubWorker getPubSub(String name, int corePoolSize, int maxPoolSize, int maxQueueSize){
        PubSubWorker worker = workers.get(name);
        if(null == worker){
            worker = new PubSubWorker(name, corePoolSize, maxPoolSize, maxQueueSize);
            workers.put(name, worker);
        }

        return worker;
    }
}
