package com.airxiechao.axcboot.communication.pubsub;

import com.airxiechao.axcboot.communication.pubsub.rabbitmq.RabbitmqPubSub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PubSubManager {

    private static final Logger logger = LoggerFactory.getLogger(PubSubManager.class);

    private static PubSubManager ourInstance = new PubSubManager();

    private Map<String, IPubSub> pubsubs = new ConcurrentHashMap<>();
    private IPubSub rabbitmqPubSub;

    public static PubSubManager getInstance() {
        return ourInstance;
    }

    private PubSubManager() {
    }

    public IPubSub createPubSub(String name, int corePoolSize, int maxPoolSize, int maxQueueSize){
        IPubSub pubSub = pubsubs.get(name);
        if(null == pubSub){
            pubSub = new PubSubWorker(name, corePoolSize, maxPoolSize, maxQueueSize);
            pubsubs.put(name, pubSub);
        }

        return pubSub;
    }

    public IPubSub getPubSub(String name){
        return pubsubs.get(name);
    }

    public IPubSub createRabbitmq(String host, int port, String username, String password, String virtualHost){
        if(null == rabbitmqPubSub){
            rabbitmqPubSub = new RabbitmqPubSub(host, port, username, password, virtualHost);
        }

        return rabbitmqPubSub;
    }

    public IPubSub getRabbitmq(){
        return rabbitmqPubSub;
    }
}
