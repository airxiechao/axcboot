# axcboot
a collection of java back-end components

## Maven

```xml
<dependency>
    <groupId>io.github.airxiechao</groupId>
    <artifactId>axcboot</artifactId>
    <version>${axcboot.version}</version>
</dependency>
```

## Sub-projects

- axcdevops: remote run devops commands [https://github.com/airxiechao/axcdevops]

## Communication

- rpc 远程通信

```java
import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.rpc.client.RpcClient;
import com.airxiechao.axcboot.communication.rpc.server.RpcServer;

import java.util.HashMap;
import java.util.Map;

public class RpcTestServer
{
    public static void main( String[] args )
    {
        RpcServer server = new RpcServer("server1")
                .config("127.0.0.1", 8888, 2, 16, 10, null);


        server.registerRpcHandler("add", (ctx, payload) -> {

            Response resp = new Response();
            resp.success();
            resp.setData((Integer)payload.get("a") + (Integer)payload.get("b"));

            return resp;
        });

        server.syncStart();

        //for(int i = 0; i < 10; ++i){
        while (true){
            System.out.println("-------");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Map map = new HashMap();
            map.put("a", 1);
            map.put("b", 2);
            try{
                Response ret = server.callClient("client1", "add2", map);
                System.out.println(ret.getCode()+", "+ret.getMessage()+", "+ret.getData());
            }catch (Exception e){
                System.out.println(e.getMessage());
            }

        }

        //server.syncStop();
    }
}

class Client{
    public static void main( String[] args ){
        RpcClient client = new RpcClient("client1")
                .config("127.0.0.1", 8888, 16, 1, 10, null);

        client.registerRpcHandler("add2", (ctx, payload) -> {

            Response resp = new Response();
            resp.success();
            resp.setData((Integer)payload.get("a") + (Integer)payload.get("b") + 2);

            return resp;
        });
        client.syncStart();


        //for(int i = 0; i < 2; ++i){
        while(true) {

            System.out.println("++++++++");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Map map = new HashMap();
            map.put("a", 1);
            map.put("b", 2);
            try{
                Response ret = client.callServer("add", map);
                System.out.println(ret.getCode()+", "+ret.getMessage()+", "+ret.getData());
            }catch (Exception e){

            }
        }

        //client.syncStop();
    }
}
```

- ltc 线程通信
```java
import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.ltc.CallableWorker;
import com.airxiechao.axcboot.communication.ltc.WorkerManager;
import com.airxiechao.axcboot.communication.common.annotation.Query;

import java.util.HashMap;
import java.util.Map;


public class LtcTest {

    public static class TestWorker extends CallableWorker{

        public TestWorker() {
            super("test-worker", 10, 100, 100);

            registerHandler(TestWorkerHandler.class);

            run();
        }
    }

    public static class TestWorkerHandler {

        @Query("add")
        public static Integer add(Map params){
            Integer a = (Integer)params.get("a");
            Integer b = (Integer)params.get("b");
            
            return a+b;
        }
    }

    public static void main(String[] args){
        CallableWorker worker = WorkerManager.getInstance().getWorker(TestWorker.class);

        Map params = new HashMap();
        params.put("a", 1);
        params.put("b", 2);
        Response resp = worker.request("add", params);
        System.out.println(resp.getData());
    }
}
```

- pubsub 发布订阅
```java
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
```

- rest 微服务
```java
import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.rest.annotation.Get;
import com.airxiechao.axcboot.communication.common.annotation.Param;
import com.airxiechao.axcboot.communication.rest.security.AuthPrincipal;
import com.airxiechao.axcboot.communication.rest.server.RestServer;
import com.airxiechao.axcboot.communication.rest.util.RestUtil;
import io.undertow.server.HttpServerExchange;

public class RestTestServer {

    public static void main(String[] args){
        RestServer restServer = new RestServer("0.0.0.0", 80,
            (AuthPrincipal principal, String[] roles) -> {
                return false;
            });

        restServer.registerHandler(RestHandler.class)
                .registerStatic("/", "html",
                        "index.html", "login.html", null);

        restServer.start();
    }

}

class RestHandler {

    /**
     * 加法：/rest/add?a=1&b=2
     * @param exchange
     * @return
     */
    @Get("/add")
    @Param(value = "a", required = true)
    @Param(value = "b", required = true)
    public static Response hello(HttpServerExchange exchange) {

        Integer a = RestUtil.queryIntegerParam(exchange, "a");
        Integer b = RestUtil.queryIntegerParam(exchange, "b");

        Response resp = new Response();
        resp.success();
        resp.setData(a+b);

        return resp;
    }
}
```

- websocket
```java
import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.websocket.annotation.WsMessageType;
import com.airxiechao.axcboot.communication.websocket.server.WsServer;
import com.alibaba.fastjson.JSON;
import io.undertow.websockets.core.WebSocketChannel;

import java.util.Map;

public class WsServerTest {

    public static void main(String args[]){
        WsServer wsServer = new WsServer("0.0.0.0", 80, "/ws");
        wsServer.registerHandler(ParkWsHandler.class);
        wsServer.start();
    }
}

class ParkWsHandler {

    @WsMessageType("add")
    public static Object add(Object payload, WsServer server, WebSocketChannel channel){
        Map map = JSON.parseObject((String)payload, Map.class);

        Response resp = new Response();
        resp.success();
        resp.setData((Integer)map.get("a")+(Integer)map.get("b"));

        return resp;
    }
}
```

```javascript
// client
let ws = new WebSocket('ws://127.0.0.1:80/ws');
ws.onopen = function (event) {
    let req = {
        requestId: 1,
        type: 'add',
        payload: {a:1,b:2},
    }
    ws.send(JSON.stringify(req))
};
ws.onmessage = function (event) {
    let msg = JSON.parse(event.data);
    //console.log(resp)

    let type = msg.type
    let payload = msg.payload
    if(payload.code === '0'){
        if(type == 'add'){
            console.log(payload.data)
        }
    }
    

};
ws.onclose = function(){
    console.log('websocket is closed');
};
```

## Storage

- db 多数据源数据库
```java

import com.airxiechao.axcboot.storage.annotation.Table;
import com.airxiechao.axcboot.storage.db.DbManager;

public class DbTest {

    @Table(value = "x", datasource = "datasource1")
    public static class X {

        private int id;
        private String value;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @Table(value = "y", datasourceMethod = "getDatasource")
    public static class Y {

        public static String getDatasource(){
            return "datasource2";
        }

        private int id;
        private String value;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static void main(String[] args){
        X x = new X();
        x.setValue("111");

        Y y = new Y();
        y.setValue("222");

        DbManager.getInstance().insert(x);
        DbManager.getInstance().insert(y);
    }

}
```

- cache 缓存
```java
// expire cache
ExpiringCacheManager.getInstance().createCache("expire", 5, ExpiringCache.UNIT.SECOND);
ExpiringCache<Integer> expiringCache = ExpiringCacheManager.getInstance().getCache("expire");
expiringCache.put("key", 1);
Integer value = expiringCache.get("key");

// db cache
DbCache<T> dbCache = DbCacheManager.getInstance().getCache(T.class);
T t = dbCache.get(1L);

// memory cache
MemoryCache memoryCache = MemoryCacheManager.getInstance().getCache("memory");
memoryCache.put("key2", 1);
int value2 = (Integer) memoryCache.get("key2");
```

## Process

- transaction 分布式事务
```java
import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.process.transaction.TransactionPipeline;

import java.util.Map;

public class TransactionTest {

    public static void main(String[] args){
        TransactionPipeline pipeline = new TransactionPipeline("test-pipeline");

        Map map = pipeline.getTranStore();
        map.put("a", 1);

        pipeline.addStep("step-1", (stepStore, tranStore, retStore, tlog)->{
            tlog.info("add 1");

            Integer a = (Integer) tranStore.get("a");
            tranStore.put("a", a+1);
        }, (stepStore, tranStore, retStore, tlog)->{
            tlog.info("rollback");

            Integer a = (Integer) tranStore.get("a");
            tranStore.put("a", a-1);
        });

        pipeline.addStep("step-2", (stepStore, tranStore, retStore, tlog)->{
            tlog.info("add 2");

            Integer a = (Integer) tranStore.get("a");
            tranStore.put("a", a+2);

            retStore.put("ret", a+2);
        }, (stepStore, tranStore, retStore, tlog)->{
            tlog.info("rollback");

            Integer a = (Integer) tranStore.get("a");
            tranStore.put("a", a-2);
        });

        Response resp = pipeline.execute();
        Map ret = (Map)resp.getData();
        System.out.println(ret.get("ret"));
    }
}
```

- schedule 定时任务
```java
ScheduleTask scheduleTask = ScheduleTaskManager.getInstance().getScheduleTask("scheduler", 1);
scheduleTask.shceduleEveryPeriod(1, TimeUnit.SECONDS, ()->{
    System.out.println("task run");
});
```

## Util

- crypto des加解密

- codec 编解码工具

- model 对象拷贝工具

- time 时间格式工具

- string 字符串工具
