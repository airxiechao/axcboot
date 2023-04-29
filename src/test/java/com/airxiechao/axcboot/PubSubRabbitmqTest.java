package com.airxiechao.axcboot;

import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.pubsub.IPubSub;
import com.airxiechao.axcboot.communication.pubsub.PubSubManager;
import com.airxiechao.axcboot.util.MapBuilder;

public class PubSubRabbitmqTest {
    public static void main(String[] args) throws InterruptedException {

        IPubSub pubsub = PubSubManager.getInstance().createRabbitmq(
                "localhost", 5672, "admin", "123456", "/");

        pubsub.subscribe("e1", "a", map -> {

            String p = (String)map.get("p");
            System.out.println("a -> "+p);

            return new Response();
        });

        pubsub.subscribe("e1", "b", map -> {

            String p = (String)map.get("p");
            System.out.println("b -> "+p);

            return new Response();
        });

        int i = 100;
        while (true){
            pubsub.publish("e1", new MapBuilder()
                    .put("p", i++ + "")
                    .build());

            if(i == 200){
                pubsub.unsubscribe("e1", "a");
            }

            Thread.sleep(1000);
        }

    }
}
