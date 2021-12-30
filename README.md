# axcboot
a collection of java backend components

## Maven

```xml
<dependency>
    <groupId>io.github.airxiechao</groupId>
    <artifactId>axcboot</artifactId>
    <version>${axcboot.version}</version>
</dependency>
```

## Communication

- rpc 远程通信

```java
import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.common.annotation.Query;
import com.airxiechao.axcboot.communication.rpc.client.RpcClient;
import com.airxiechao.axcboot.communication.rpc.common.RpcExchange;
import com.airxiechao.axcboot.communication.rpc.server.RpcServer;
import com.airxiechao.axcboot.communication.rpc.util.RpcUtil;
import com.airxiechao.axcboot.core.annotation.IRpc;
import com.airxiechao.axcboot.core.rpc.RpcClientCaller;
import com.airxiechao.axcboot.core.rpc.RpcReg;
import com.airxiechao.axcboot.core.rpc.RpcServerCaller;
import com.airxiechao.axcboot.crypto.SslUtil;
import com.airxiechao.axcboot.storage.fs.LocalFs;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class RpcTestServer
{
    public static void main( String[] args ) throws Exception {
        SelfSignedCertificate ssc = new SelfSignedCertificate();

        RpcServer server = new RpcServer("server1")
                //.setVerboseLog(true)
                .config("127.0.0.1", 8888, 2, 16, null, null, null)
                .useSsl(SslUtil.buildKeyManagerFactory(new LocalFs("d:/test/ssl"), "rpc-server-key.jks", "123456"),
                        SslUtil.buildReloadableTrustManager(new LocalFs("d:/test/ssl"), "rpc-server-trust.jks", "123456"));

//        server.registerRpcHandler("add", (ctx, payload) -> {
//
//            Response resp = new Response();
//            resp.success();
//            resp.setData((Integer)payload.get("a") + (Integer)payload.get("b"));
//
//            return resp;
//        });

        RpcReg rpcReg = new RpcReg("com.airxiechao.axcboot", server);
        rpcReg.registerHandlerIfExists(null);

        server.syncStart();

        RpcServerCaller caller = new RpcServerCaller(server);

        //for(int i = 0; i < 10; ++i){
        while (true){
            System.out.println("-------");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            AddParam param = new AddParam();
            param.setA(1);
            param.setB(2);
            try{
                if(server.getActiveClients().size() > 0) {
                    Response ret = caller.get(IAddRpc.class, server.getActiveClients().get(0)).add2(param);
                    System.out.println(ret.getCode() + ", " + ret.getMessage() + ", " + ret.getData());
                }
            }catch (Exception e){
                System.out.println(e.getMessage());
            }

        }

        //server.syncStop();
    }
}

class RpcTestClient{
    public static void main( String[] args ) throws Exception {
        SelfSignedCertificate ssc = new SelfSignedCertificate();

        RpcClient client = new RpcClient("client1")
                //.setVerboseLog(true)
                .config("127.0.0.1", 8888, 16, 10,
                        null, null, null)
                .useSsl(SslUtil.buildKeyManagerFactory(new LocalFs("d:/test/ssl"), "rpc-client-key.jks", "123456"),
                        SslUtil.buildReloadableTrustManager(new LocalFs("d:/test/ssl"), "rpc-client-trust.jks", "123456"));

//        client.registerRpcHandler("add2", (ctx, payload) -> {
//
//            Response resp = new Response();
//            resp.success();
//            resp.setData((Integer)payload.get("a") + (Integer)payload.get("b") + 2);
//
//            return resp;
//        });

        RpcReg rpcReg = new RpcReg("com.airxiechao.axcboot", client);
        rpcReg.registerHandlerIfExists(null);

        client.syncStart();

        RpcClientCaller caller = new RpcClientCaller(client);

        //for(int i = 0; i < 2; ++i){
        while(true) {

            System.out.println("++++++++");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            AddParam param = new AddParam();
            param.setA(1);
            param.setB(2);
            try{
                Response ret = caller.get(IAddRpc.class).add(param);
                System.out.println(ret.getCode()+", "+ret.getMessage()+", "+ret.getData());
            }catch (Exception e){

            }
        }

        //client.syncStop();
    }
}

@IRpc
interface IAddRpc {

    @Query("add")
    Response add(Object param);

    @Query("add2")
    Response add2(Object param);
}

class AddParam{
    private Integer a;
    private Integer b;

    public Integer getA() {
        return a;
    }

    public void setA(Integer a) {
        this.a = a;
    }

    public Integer getB() {
        return b;
    }

    public void setB(Integer b) {
        this.b = b;
    }
}

class AddRpcHandler implements IAddRpc {

    @Override
    public Response add(Object param) {
        RpcExchange rpcExchange = (RpcExchange) param;
        AddParam addParam = RpcUtil.getObjectParam(rpcExchange, AddParam.class);
        return new Response().data(addParam.getA() + addParam.getB());
    }

    @Override
    public Response add2(Object param) {

        RpcExchange rpcExchange = (RpcExchange) param;
        AddParam addParam = RpcUtil.getObjectParam(rpcExchange, AddParam.class);
        return new Response().data(addParam.getA() + addParam.getB() + 2);
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

        PubSubWorker worker = PubSubManager.getInstance().createPubSub("test-pubsub", 2, 5, 10);

        worker.subscribe("e1", "a", map -> {

            String p = (String) map.get("p");
            System.out.println("a -> " + p);

            return new Response();
        });

        worker.subscribe("e1", "b", map -> {

            String p = (String) map.get("p");
            System.out.println("b -> " + p);

            return new Response();
        });

        while (true) {
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
import com.airxiechao.axcboot.communication.rest.server.RestServer;
import com.airxiechao.axcboot.communication.rest.util.RestUtil;
import com.airxiechao.axcboot.util.AnnotationUtil;
import com.airxiechao.axcboot.util.HttpUtil;
import com.airxiechao.axcboot.util.MapBuilder;
import com.airxiechao.axcboot.util.ProxyUtil;
import com.alibaba.fastjson.JSON;
import io.undertow.server.HttpServerExchange;

import java.util.Map;

public class RestTestServer {

    public static void main(String[] args) throws InterruptedException {
        RestServer restServer = new RestServer("test");
        restServer.config("0.0.0.0", 80, null, null, null,
                (exchange, principal, roles) -> {
                    return false;
                });

        restServer
                .registerHandler(RestHandler.class)
                .registerStatic("/", "html",
                        "index.html", "login.html", null);

        restServer.start();

        Thread.sleep(1000);

        Map params = new MapBuilder<String, String>()
                .put("a", "1")
                .put("b", "2")
                .build();
        Response resp = RestClient.get(Add.class).add(params);
        System.out.println(resp.getData());
    }

}

interface Add {
    /**
     * 加法：/api/add?a=1&b=2
     * @param exc
     * @return
     */
    @Get("/add")
    @Param(value = "a", required = true)
    @Param(value = "b", required = true)
    Response add(Object exc);
}

class RestHandler implements Add {

    @Override
    public Response add(Object exc) {

        HttpServerExchange exchange = (HttpServerExchange) exc;

        Integer a = RestUtil.queryIntegerParam(exchange, "a");
        Integer b = RestUtil.queryIntegerParam(exchange, "b");

        Response resp = new Response();
        resp.success();
        resp.setData(a + b);

        return resp;
    }
}

class RestClient {
    public static <T> T get(Class<T> cls) {
        return ProxyUtil.buildProxy(cls, (proxy, method, args) -> {
            String path = AnnotationUtil.getMethodAnnotation(method, Get.class).value();
            Map params = (Map) args[0];

            String ret = HttpUtil.get("http://127.0.0.1/api" + path, params);
            return JSON.parseObject(ret, Response.class);
        });
    }
}
```

- websocket

```java
import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.websocket.annotation.WsHandler;
import com.airxiechao.axcboot.communication.websocket.annotation.WsHandlerserver.WsServer;
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

    @WsHandler("add")
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
import com.airxiechao.axcboot.storage.db.sql.DbManager;

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

        public static String getDatasource() {
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

    public static void main(String[] args) {
        X x = new X();
        x.setValue("111");

        Y y = new Y();
        y.setValue("222");

        DbManager dbManager = new DbManager();

        dbManager.insert(x);
        dbManager.insert(y);
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
