package com.airxiechao.axcboot.communication.pubsub;

import com.airxiechao.axcboot.communication.common.annotation.Param;
import com.airxiechao.axcboot.communication.common.annotation.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class PublishHandler {

    private static final Logger logger = LoggerFactory.getLogger(PublishHandler.class);

    public static final String QUERY_PUBLISH = "publish";
    public static final String QUERY_PUBLISH_PARAM_EVENT = "__event";
    public static final String QUERY_PUBLISH_PARAM_WORKER = "__worker";

    @Query(QUERY_PUBLISH)
    @Param(value = QUERY_PUBLISH_PARAM_EVENT, required = true)
    @Param(value = QUERY_PUBLISH_PARAM_WORKER, required = true)
    public static void publish(Map<String, Object> params){

        String event = (String)params.remove(QUERY_PUBLISH_PARAM_EVENT);
        PubSubWorker worker = (PubSubWorker)params.remove(QUERY_PUBLISH_PARAM_WORKER);

        logger.info("pubsub worker [{}] publish event [{}]", worker.getName(), event);

        Map<String, ISubscriber> subs = null;
        try {
            subs = worker.getMatchedSubscriber(event);
        } catch (Exception e) {
            logger.error("pubsub worker [{}] handle event [{}] error",
                    worker.getName(), event, e);
            return;
        }

        for(Map.Entry<String, ISubscriber> entry : subs.entrySet()){
            String sub = entry.getKey();
            ISubscriber handler = entry.getValue();

            try {
                handler.handle(params);
            } catch (Exception e) {
                logger.error("pubsub worker [{}] subscriber [{}] handle event [{}] error",
                        worker.getName(), sub, event, e);
            }
        }
    }
}
