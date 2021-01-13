package com.airxiechao.axcboot;

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
                .config("127.0.0.1", 8888, 2, 16, null);


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
                .config("127.0.0.1", 8888, 16, 1,
                        null, null);

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