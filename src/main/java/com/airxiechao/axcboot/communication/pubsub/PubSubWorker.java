package com.airxiechao.axcboot.communication.pubsub;

import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.ltc.CallableWorker;
import com.airxiechao.axcboot.communication.pubsub.annotation.Sink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.airxiechao.axcboot.communication.pubsub.PublishHandler.*;

public class PubSubWorker extends CallableWorker {

    private static final Logger logger = LoggerFactory.getLogger(PubSubWorker.class);

    private Map<String, Map<String, ISubscriber>> subscribers = new ConcurrentHashMap<>();

    public PubSubWorker(String name, int corePoolSize, int maxPoolSize, int maxQueueSize) {
        super(name, corePoolSize, maxPoolSize, maxQueueSize);

        // 注册发布处理器
        this.registerHandler(PublishHandler.class);

        this.run();
    }

    /**
     * 异步发布
     * @param event
     * @param params
     * @return
     */
    public Response publish(String event, Map<String, Object> params){

        params.put(QUERY_PUBLISH_PARAM_EVENT, event);
        params.put(QUERY_PUBLISH_PARAM_WORKER, this);

        return this.asyncRequest(QUERY_PUBLISH, params);
    }

    /**
     * 订阅
     * @param event
     * @param subscriber
     * @param handler
     */
    public synchronized void subscribe(String event, String subscriber, ISubscriber handler){

        logger.info("pubsub worker [{}] subscriber [{}] subscribe [{}]",
                getName(), subscriber, event);

        Map<String, ISubscriber> map = getSubscriber(event);
        if(null == map){
            map = new ConcurrentHashMap<>();
            this.subscribers.put(event, map);
        }

        map.put(subscriber, handler);
    }

    /**
     * 取消订阅
     * @param event
     * @param subscriber
     */
    public void unsubscribe(String event, String subscriber){

        Map<String, ISubscriber> map = getSubscriber(event);
        if(null == map){
            return;
        }

        if(!map.containsKey(subscriber)){
            return;
        }

        logger.info("pubsub worker [{}] subscriber [{}] unsubscribe [{}]",
                getName(), subscriber, event);

        map.remove(subscriber);
    }

    /**
     * 注册消息下沉端
     */
    public void registerSink(Class<? extends ISubscriber> cls){
        Sink meta = cls.getAnnotation(Sink.class);
        if(null != meta){
            String[] events = meta.events();
            String subscriber = meta.name();

            Arrays.stream(events).forEach(event -> {
                try {
                    this.subscribe(event, subscriber, cls.getConstructor().newInstance());
                } catch (Exception e) {
                    logger.error("pubsub worker [{}] subscriber [{}] register sink [{}] error",
                            getName(), subscriber, event, e);
                }
            });
        }
    }

    /**
     * 取消消息下沉端
     * @param cls
     */
    public void unregisterSink(Class<? extends ISubscriber> cls){
        Sink meta = cls.getAnnotation(Sink.class);
        if(null != meta){
            String[] events = meta.events();
            String subscriber = meta.name();

            Arrays.stream(events).forEach(event -> {
                this.unsubscribe(event, subscriber);
            });
        }
    }


    /**
     * 获取订阅者
     * @param event
     * @return
     */
    public Map<String, ISubscriber> getSubscriber(String event){
        return this.subscribers.get(event);
    }

    /**
     * 取消所有订阅者
     * @param event
     */
    public void clearSubscriber(String event){
        logger.info("pubsub worker [{}] clear [{}] subscribers",
                getName(), event);

        this.subscribers.remove(event);
    }

    public void clearSubscriber(){
        logger.info("pubsub worker [{}] clear all subscribers",
                getName());

        this.subscribers.clear();
    }
}
