package com.airxiechao.axcboot.communication.pubsub.rabbitmq;

import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.pubsub.IPubSub;
import com.airxiechao.axcboot.communication.pubsub.ISubscriber;
import com.airxiechao.axcboot.communication.pubsub.annotation.Sink;
import com.airxiechao.axcboot.util.StringUtil;
import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RabbitmqPubSub implements IPubSub {

    private static final Logger logger = LoggerFactory.getLogger(RabbitmqPubSub.class);

    public static final int DEFAULT_PORT = 5672;
    public static final String DEFAULT_VIRTUAL_HOST = "/";

    private String host;
    private int port;
    private String username;
    private String password;
    private String virtualHost;
    private ConnectionFactory connectionFactory;
    private Connection connection;
    private Map<String, Map<String, Channel>> subscriberChannelMap = new ConcurrentHashMap<>();

    public RabbitmqPubSub(String host, int port, String username, String password, String virtualHost) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.virtualHost = virtualHost;

        this.connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setVirtualHost(virtualHost);
    }

    @Override
    public Response publish(String event, Map<String, Object> params) {
        try{
            Channel channel = getSubscriberChannel(event, "publisher");

            String exchangeName = event.split("\\.")[0];
            channel.exchangeDeclare(exchangeName, "topic");
            String message = JSON.toJSONString(params);
            channel.basicPublish(exchangeName, event, null, message.getBytes("UTF-8"));
            return new Response();
        }catch (Exception e){
            return new Response().error(e.getMessage());
        }
    }

    @Override
    public void subscribe(String event, String subscriber, ISubscriber handler) {

        try{
            Channel channel = getSubscriberChannel(event, subscriber);

            String exchangeName = event.split("\\.")[0];
            channel.exchangeDeclare(exchangeName, "topic");
            String queueName = buildSubscriberQueueName(event, subscriber);
            channel.queueDeclare(queueName, false, true, true, null);
            channel.queueBind(queueName, exchangeName, event);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");

                try {
                    Map<String, Object> params = JSON.parseObject(message, Map.class);
                    handler.handle(params);
                } catch (Exception e) {
                    logger.error("rabbitmq handle event error", e);
                }
            };
            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> { });
        }catch (Exception e){
            logger.error("rabbitmq subscribe error", e);
        }

    }

    @Override
    public void unsubscribe(String event, String subscriber) {
        try{
            Map<String, Channel> channelMap = subscriberChannelMap.get(event);
            if(null == channelMap){
                return;
            }

            Channel channel = channelMap.get(subscriber);
            if(null == channel){
                return;
            }

            if(!channel.isOpen()){
                channelMap.remove(subscriber);
                return;
            }

            channel.close();
            channelMap.remove(subscriber);

        }catch (Exception e){
            logger.error("rabbitmq unsubscribe error", e);
        }
    }

    @Override
    public void registerSink(Class<? extends ISubscriber> cls) {
        Sink meta = cls.getAnnotation(Sink.class);
        if(null != meta){
            String[] events = meta.events();
            String name = meta.name();
            String subscriber = StringUtil.isBlank(name) ? cls.getSimpleName() : name;

            Arrays.stream(events).forEach(event -> {
                try {
                    this.subscribe(event, subscriber, cls.getConstructor().newInstance());
                } catch (Exception e) {
                    logger.error("rabbitmq subscriber [{}] register sink [{}] error",
                            subscriber, event, e);
                }
            });
        }
    }

    @Override
    public void unregisterSink(Class<? extends ISubscriber> cls) {
        Sink meta = cls.getAnnotation(Sink.class);
        if(null != meta){
            String[] events = meta.events();
            String name = meta.name();
            String subscriber = StringUtil.isBlank(name) ? cls.getSimpleName() : name;

            Arrays.stream(events).forEach(event -> {
                this.unsubscribe(event, subscriber);
            });
        }
    }

    @Override
    public void clearSubscriber(String event) {
        Map<String, Channel> channelMap =  subscriberChannelMap.get(event);
        if(null == channelMap){
            return;
        }

        channelMap.keySet().forEach(subscriber -> unsubscribe(event, subscriber));

        subscriberChannelMap.remove(event);
    }

    @Override
    public void clearSubscriber() {
        subscriberChannelMap.keySet().forEach(event -> {
            subscriberChannelMap.get(event).keySet().forEach(subscriber -> {
                unsubscribe(event, subscriber);
            });
        });

        subscriberChannelMap.clear();
    }

    private String buildSubscriberQueueName(String event, String subscriber){
        return event + ":" + subscriber;
    }

    private Channel getSubscriberChannel(String event, String subscriber){
        Map<String, Channel> channelMap = subscriberChannelMap.get(event);
        if(null == channelMap){
            channelMap = new ConcurrentHashMap<>();
            subscriberChannelMap.put(event, channelMap);
        }

        Channel channel = channelMap.get(subscriber);
        if(null == channel || !channel.isOpen()){
            try {
                channel = getConnection().createChannel();
                channelMap.put(subscriber, channel);
            } catch (Exception e) {
                throw new RuntimeException("rabbitmq create subscriber channel error", e);
            }
        }

        return channel;
    }

    private Connection getConnection(){
        if(null == this.connection || !this.connection.isOpen()){
            try {
                this.connection = this.connectionFactory.newConnection();
            } catch (Exception e) {
                throw new RuntimeException("connect rabbitmq error", e);
            }
        }

        return this.connection;
    }
}
