package com.airxiechao.axcboot.communication.rpc.client;

import com.airxiechao.axcboot.communication.common.RequestId;
import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.common.annotation.Query;
import com.airxiechao.axcboot.communication.rpc.common.*;
import com.airxiechao.axcboot.communication.rpc.security.IRpcAuthChecker;
import com.airxiechao.axcboot.communication.rpc.util.RpcUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import static com.airxiechao.axcboot.communication.rpc.common.RpcContext.HEARTBEAT_PERIOD_SECS;

public class RpcClient {

    private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);

    public enum RPC_CLIENT_STATUS {
        NOT_STARTED, NOT_CONNECTED, CONNECTED, STOPPED
    }

    protected String name;
    protected String serverIp;
    protected int serverPort;
    protected int numIoThreads;
    protected int numWorkerThreads;
    protected Bootstrap clientBootstrap;
    protected NioEventLoopGroup clientGroup;
    protected Map<String, IRpcMessageHandler> serviceHandlers = new HashMap<>();
    protected RpcClientMessageRouter router;
    protected IRpcEventListener connectListener;

    protected int reconnectDelaySecs = 5;
    private int callTimeoutSecs = 10;
    private IRpcAuthChecker authChecker;

    protected RPC_CLIENT_STATUS status = RPC_CLIENT_STATUS.NOT_STARTED;

    private ScheduledExecutorService scheduledExecutorService;

    public RpcClient(String name){

        this.name = name;

        this.scheduledExecutorService = new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder()
                .setNameFormat("rpc-client-["+this.name+"]-schedule")
                .setDaemon(true)
                .build()
        );
    }

    public RpcClient config(
            String ip,
            int port,
            int numWorkerThreads,
            int reconnectDelaySecs,
            int callTimeoutSecs,
            IRpcAuthChecker authChecker,
            IRpcEventListener connectListener
    ){
        this.serverIp = ip;
        this.serverPort = port;
        this.numIoThreads = 1;
        this.numWorkerThreads = numWorkerThreads;
        this.reconnectDelaySecs = reconnectDelaySecs;
        this.callTimeoutSecs = callTimeoutSecs;
        this.authChecker = authChecker;
        this.connectListener = connectListener;

        return this;
    }

    /**
     * 启动
     * @return
     */
    public synchronized RpcFuture start(){
        RpcFuture rpcFuture = new RpcFuture();

        if (RPC_CLIENT_STATUS.NOT_STARTED != getStatus()) {
            rpcFuture.fail(new Exception("rpc client is at NOT_STARTED status"));
            return rpcFuture;
        }

        setStatus(RPC_CLIENT_STATUS.NOT_CONNECTED);

        clientBootstrap = new Bootstrap();
        clientGroup = new NioEventLoopGroup(this.numIoThreads);
        clientBootstrap.group(clientGroup);
        RpcMessageEncoder encoder = new RpcMessageEncoder();
        router = new RpcClientMessageRouter(this);
        clientBootstrap.channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipe = ch.pipeline();
                pipe.addLast(new ReadTimeoutHandler(HEARTBEAT_PERIOD_SECS * 2));
                pipe.addLast(new RpcMessageDecoder());
                pipe.addLast(encoder);
                pipe.addLast(router);
            }

        });

        initHeartbeat();
        rpcFuture = connect();

        return rpcFuture;
    }

    public boolean syncStart(){
        try {
            Response resp = start().get();
            return resp.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 连接
     * @return
     */
    public RpcFuture connect() {

        RpcFuture rpcFuture = new RpcFuture();

        if (RPC_CLIENT_STATUS.NOT_CONNECTED != getStatus()) {
            rpcFuture.fail(new Exception("rpc client is not at NOT_CONNECTED status"));
            return rpcFuture;
        }

        clientBootstrap.connect(serverIp, serverPort).addListener(future -> {
            if (future.isSuccess()) {
                setStatus(RPC_CLIENT_STATUS.CONNECTED);

                rpcFuture.success(new Response());

                logger.info("rpc-client-[{}] connect to rpc server [{}:{}] success", name, serverIp, serverPort);
            }else{
                rpcFuture.fail(future.cause());
                logger.error("rpc-client-[{}] connect to rpc server [{}:{}] fails [{}]", name, serverIp, serverPort, future.cause().getMessage());

                // 重连
                clientGroup.schedule(() -> {
                    connect();
                }, this.reconnectDelaySecs, TimeUnit.SECONDS);
            }
        });

        return rpcFuture;
    }

    /**
     * 停止
     * @return
     */
    public synchronized RpcFuture stop() {
        RpcFuture rpcFuture = new RpcFuture();

        if (RPC_CLIENT_STATUS.NOT_CONNECTED != getStatus() && RPC_CLIENT_STATUS.CONNECTED != getStatus()) {
            rpcFuture.fail(new Exception("rpc client is not at NOT_CONNECTED or CONNECTED status"));
            return rpcFuture;
        }

        setStatus(RPC_CLIENT_STATUS.STOPPED);

        scheduledExecutorService.shutdownNow();
        router.closeGracefully();
        clientGroup.shutdownGracefully().addListener(future -> {
            if (future.isSuccess()) {
                rpcFuture.success(new Response());
                logger.info("rpc-client-[{}] disconnect from rpc server [{}:{}] success", name, serverIp, serverPort);
            }else{
                rpcFuture.fail(future.cause());
                logger.error("rpc-client-[{}] disconnect from rpc server [{}:{}] fails [{}]", name, serverIp, serverPort, future.cause().getMessage());
            }

        });

        return rpcFuture;
    }

    public boolean syncStop(){
        try {
            Response resp = stop().get();
            return resp.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 设置心跳
     */
    private void initHeartbeat(){
        this.scheduledExecutorService.scheduleAtFixedRate(()->{
            heartbeat();
        }, HEARTBEAT_PERIOD_SECS, HEARTBEAT_PERIOD_SECS, TimeUnit.SECONDS);
    }

    private void heartbeat(){
        try{
            Map params = new HashMap();
            params.put("name", this.name);
            Response resp = callServer(RpcMessage.TYPE_PING, params);
            if(resp.isSuccess()){
                router.updateRpcContextLastHeartbeatTime(new Date());
                logger.info("rpc-client-[{}] heartbeat success", this.name);
            }else{
                logger.error("rpc-client-[{}] heartbeat error [{}]", this.name, resp.getMessage());
            }
        }catch (Exception e){
            logger.error("rpc-client-[{}] heartbeat error [{}]", this.name, e.getMessage());
        }
    }

    public void heartbeatOnce(){
        Thread t = new Thread(()-> heartbeat());
        t.setDaemon(true);
        t.start();
    }

    public void runConnectListener(){
        if(null != this.connectListener){
            Thread t = new Thread(()-> this.connectListener.run());
            t.setDaemon(true);
            t.start();
        }
    }

    /**
     * 注册rpc请求处理器
     * @param type
     * @param handler
     * @return
     */
    public RpcClient registerRpcHandler(String type, IRpcMessageHandler handler){
        serviceHandlers.put(type, handler);
        return this;
    }

    public RpcClient registerRpcHandler(IRpcMessageHandler handler){
        try{
            Method method = RpcUtil.getHandleMethod(handler.getClass());
            if(null != method){
                Query query = method.getAnnotation(Query.class);
                if(null != query){
                    String type = query.value();
                    serviceHandlers.put(type, handler);
                }else{
                    logger.error("register rpc handler error: no type");
                }
            }else{
                logger.error("register rpc handler error: no handle method");
            }

        }catch (Exception e){
            logger.error("register rpc handler error", e);
        }

        return this;
    }

    public RpcClient registerRpcHandler(Class<? extends IRpcMessageHandler> cls){
        try{
            Method method = RpcUtil.getHandleMethod(cls);
            if(null != method){
                Query query = method.getAnnotation(Query.class);
                if(null != query){
                    String type = query.value();
                    serviceHandlers.put(type, cls.getConstructor().newInstance());
                }else{
                    logger.error("register rpc handler error: no type");
                }
            }else{
                logger.error("register rpc handler error: no handle method");
            }

        }catch (Exception e){
            logger.error("register rpc handler error", e);
        }

        return this;
    }

    /**
     * 向服务端发送请求
     * @param type
     * @param payload
     * @return
     */
    public Response callServer(String type, Map payload){
        return callServer(type, JSON.toJSONString(payload));
    }

    private Response callServer(String type, String payload){

        Response resp = new Response();
        if(RPC_CLIENT_STATUS.CONNECTED != getStatus()){
            resp.error("rpc server is not connected");
            return resp;
        }
        if(!isServerActive()){
            resp.error("rpc server is not active");
            return resp;
        }

        try{
            String requestId = RequestId.next();
            RpcMessage message = new RpcMessage(requestId, type, payload);
            RpcFuture future = this.router.sendToServer(message);
            resp = future.get(callTimeoutSecs, TimeUnit.SECONDS);
            return resp;
        }catch (Exception e) {
            throw new RpcException(e);
        }
    }


    /**
     * 服务端是否在线
     * @return
     */
    public boolean isServerActive(){
        RpcContext ctx = this.router.getRpcContext();
        if(null != ctx && !ctx.isHeartbeatExpired()){
            return true;
        }

        return false;
    }

    public RPC_CLIENT_STATUS getStatus() {
        return status;
    }

    public void setStatus(RPC_CLIENT_STATUS status) {
        this.status = status;
    }

    public String getName(){
        return this.name;
    }

    public int getReconnectDelaySecs(){
        return this.reconnectDelaySecs;
    }

    public Map<String, IRpcMessageHandler> getServiceHandlers(){
        return serviceHandlers;
    }

    public int getNumWorkerThreads(){
        return numWorkerThreads;
    }

    public IRpcAuthChecker getAuthChecker(){
        return authChecker;
    }
}
