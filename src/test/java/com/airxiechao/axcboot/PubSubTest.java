package com.airxiechao.axcboot;

import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.pubsub.PubSubManager;
import com.airxiechao.axcboot.communication.pubsub.PubSubWorker;
import com.airxiechao.axcboot.util.MapBuilder;

public class PubSubTest {

    public static void main(String[] args) throws InterruptedException {

        PubSubWorker worker = PubSubManager.getInstance().getPubSub("test-pubsub", 2, 5, 10);

        worker.subscribe("e1", "a", map -> {

            String p = (String)map.get("p");
            System.out.println("a -> "+p);

            return new Response();
        });

        worker.subscribe("e1", "b", map -> {

            String p = (String)map.get("p");
            System.out.println("b -> "+p);

            return new Response();
        });

        while (true){
            worker.publish("e1", new MapBuilder()
                    .put("p", "111")
                    .build());

            worker.unsubscribe("e1", "a");

            Thread.sleep(1000);
        }

    }
}
