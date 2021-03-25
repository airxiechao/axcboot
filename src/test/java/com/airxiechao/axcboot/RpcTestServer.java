package com.airxiechao.axcboot;

import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.rpc.client.RpcClient;
import com.airxiechao.axcboot.communication.rpc.server.RpcServer;
import com.airxiechao.axcboot.crypto.SslUtil;
import com.airxiechao.axcboot.storage.fs.LocalFs;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLException;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

public class RpcTestServer
{
    public static void main( String[] args ) throws Exception {
        SelfSignedCertificate ssc = new SelfSignedCertificate();

        RpcServer server = new RpcServer("server1")
                //.setVerboseLog(true)
                .config("127.0.0.1", 8888, 2, 16, null, null, null)
                .useSsl(SslUtil.buildKeyManagerFactory(new LocalFs("d:/test/ssl"), "rpc-server-key.jks", "123456"),
                        SslUtil.buildReloadableTrustManager(new LocalFs("d:/test/ssl"), "rpc-server-trust.jks", "123456"));


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
                if(server.getActiveClients().size() > 0) {
                    Response ret = server.callClient(server.getActiveClients().get(0), "add2", map);
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