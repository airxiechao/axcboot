package com.airxiechao.axcboot;

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
        System.out.println(Thread.currentThread().getName());
        System.out.println(Thread.currentThread().getState());
        RpcExchange rpcExchange = (RpcExchange) param;
        AddParam addParam = RpcUtil.getObjectParam(rpcExchange, AddParam.class);
        return new Response().data(addParam.getA() + addParam.getB());
    }

    @Override
    public Response add2(Object param) {
        System.out.println(Thread.currentThread().getName());
        System.out.println(Thread.currentThread().getState());
        RpcExchange rpcExchange = (RpcExchange) param;
        AddParam addParam = RpcUtil.getObjectParam(rpcExchange, AddParam.class);
        return new Response().data(addParam.getA() + addParam.getB() + 2);
    }
}

