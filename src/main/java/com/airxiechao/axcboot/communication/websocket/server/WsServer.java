package com.airxiechao.axcboot.communication.websocket.server;

import com.airxiechao.axcboot.communication.common.security.IAuthTokenChecker;
import com.airxiechao.axcboot.communication.websocket.common.*;
import io.undertow.Undertow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.undertow.Handlers.*;

public class WsServer {

    private static final Logger logger = LoggerFactory.getLogger(WsServer.class);

    protected String name;
    protected Undertow server;
    protected String ip;
    protected String endpoint;
    protected int port;
    protected AbstractWsListener listener;
    protected IAuthTokenChecker authTokenChecker;

    public WsServer(String name){
        this.name = name;
    }

    public void config(String ip, int port, String endpoint, AbstractWsListener listener, IAuthTokenChecker authTokenChecker){
        this.ip = ip;
        this.port = port;
        this.endpoint = endpoint.startsWith("/") ? endpoint : "/" + endpoint;
        this.listener = listener;
        this.authTokenChecker = authTokenChecker;
    }

    public void start(){
        server = Undertow.builder()
                .addHttpListener(this.port, this.ip)
                .setHandler(
                        path().addPrefixPath(this.endpoint, websocket(new WsCallback(this.listener, this.authTokenChecker)))
                ).build();
        server.start();
    }

    public void stop(){
        server.stop();
    }

}
