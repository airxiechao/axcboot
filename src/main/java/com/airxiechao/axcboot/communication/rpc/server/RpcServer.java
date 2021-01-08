package com.airxiechao.axcboot.communication.rpc.server;

import com.airxiechao.axcboot.communication.common.RequestId;
import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.common.annotation.Query;
import com.airxiechao.axcboot.communication.rpc.common.*;
import com.airxiechao.axcboot.communication.rpc.security.IRpcAuthChecker;
import com.airxiechao.axcboot.communication.rpc.util.RpcUtil;
import com.alibaba.fastjson.JSON;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class RpcServer {
    private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);

    public enum RPC_SERVER_STATUS {
        NOT_STARTED, STARTED, STOPPED
    }

    private String name;
    private String serverIp;
    private int serverPort;
    private int numIoThreads;
    private int numWorkerThreads;
    private ServerBootstrap serverBootstrap;
    private EventLoopGroup serverGroup;
    private Channel serverChannel;
    private Map<String, IRpcMessageHandler> serviceHandlers = new HashMap<>();
    private RpcServerMessageRouter router;
    private int callTimeoutSecs = 10;
    private IRpcAuthChecker authChecker;

    private RPC_SERVER_STATUS status = RPC_SERVER_STATUS.NOT_STARTED;

    public RpcServer(String name){
        this.name = name;
    }

    public RpcServer config(
            String ip,
            int port,
            int numIoThreads,
            int numWorkerThreads,
            int callTimeoutSecs,
            IRpcAuthChecker authChecker
    ){
        this.serverIp = ip;
        this.serverPort = port;
        this.numIoThreads = numIoThreads;
        this.numWorkerThreads = numWorkerThreads;
        this.callTimeoutSecs = callTimeoutSecs;
        this.authChecker = authChecker;

        registerHeartbeatHandler();

        return this;
    }

    /**
     * 启动
     */
    public synchronized RpcFuture start(){

        RpcFuture rpcFuture = new RpcFuture();

        if(RPC_SERVER_STATUS.NOT_STARTED != getStatus()){
            rpcFuture.fail(new Exception("rpc server is not at NOT_STARTED status"));
            return rpcFuture;
        }

        serverBootstrap = new ServerBootstrap();
        serverGroup = new NioEventLoopGroup(this.numIoThreads);
        serverBootstrap.group(serverGroup);
        RpcMessageEncoder encoder = new RpcMessageEncoder();
        router = new RpcServerMessageRouter(this);
        serverBootstrap.channel(NioServerSocketChannel.class).childHandler(
                new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipe = socketChannel.pipeline();
                        pipe.addLast(new ReadTimeoutHandler(RpcContext.HEARTBEAT_PERIOD_SECS * 2));
                        pipe.addLast(new RpcMessageDecoder());
                        pipe.addLast(encoder);
                        pipe.addLast(router);
                    }
                }
        );

        serverChannel = serverBootstrap.bind(this.serverIp, this.serverPort).addListener(future -> {
            if (future.isSuccess()) {
                setStatus(RPC_SERVER_STATUS.STARTED);

                rpcFuture.success(new Response());
                logger.info("rpc-server-[{}] has started at [{}:{}]", this.name, this.serverIp, this.serverPort);
            }else{
                rpcFuture.fail(future.cause());
                logger.error("rpc-server-[{}] start at [{}:{}] fails", this.name, this.serverIp, this.serverPort, future.cause());
            }
        }).channel();

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
     * 停止
     */
    public synchronized RpcFuture stop(){

        RpcFuture rpcFuture = new RpcFuture();

        if(RPC_SERVER_STATUS.STARTED != getStatus()){
            rpcFuture.fail(new Exception("rpc server is not at STARTED status"));
        }

        router.closeGracefully();
        serverGroup.shutdownGracefully();
        serverChannel.close().addListener(future -> {
            if (future.isSuccess()) {
                rpcFuture.success(new Response());
                logger.info("rpc-server-[{}] has stopped", this.name);
            }else{
                rpcFuture.fail(future.cause());
                logger.error("rpc-server-[{}] stop fails", this.name, future.cause());
            }
        });

        setStatus(RPC_SERVER_STATUS.STOPPED);

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
     * 注册rpc请求处理器
     * @param type
     * @param handler
     * @return
     */
    public RpcServer registerRpcHandler(String type, IRpcMessageHandler handler){
        serviceHandlers.put(type, handler);
        return this;
    }

    public RpcServer registerRpcHandler(IRpcMessageHandler handler){
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

    public RpcServer registerRpcHandler(Class<? extends IRpcMessageHandler> cls){
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
     * 注册心跳处理
     */
    private void registerHeartbeatHandler(){
        registerRpcHandler(RpcMessage.TYPE_PING, (ctx, payload) -> {
            String clientName = getRouter().getClientByContext(ctx);
            String name = (String)payload.get("name");

            logger.info("rpc-server-[{}] receives heartbeat from rpc-client-[{}, {}]", this.name, name, clientName);

            // 如果已经存在相同名称的其他客户端，则断开连接
            router.updateOrCloseRpcContext(clientName, ctx);

            Response resp = new Response();
            resp.success();

            return resp;
        });
    }

    /**
     * 向客户端发送请求
     * @param client
     * @param type
     * @param payload
     * @return
     */
    public Response callClient(String client, String type, Map payload){
        return callClient(client, type, JSON.toJSONString(payload));
    }

    private Response callClient(String client, String type, String payload){

        Response resp = new Response();
        if(RPC_SERVER_STATUS.STARTED != getStatus()){
            resp.error("rpc server is not started");
            return resp;
        }
        if(!isClientActive(client)){
            resp.error("rpc-client-["+client+"] is not active");
            return resp;
        }

        try{
            String requestId = RequestId.next();
            RpcMessage message = new RpcMessage(requestId, type, payload);
            RpcFuture future = this.router.sendToClient(client, message);
            resp = future.get(callTimeoutSecs, TimeUnit.SECONDS);
            return resp;
        }catch (Exception e) {
            throw new RpcException(e);
        }
    }

    /**
     * 获取在线客户端
     * @return
     */
    public List<String> getActiveClients(){
        if(null == router){
            return new ArrayList<>();
        }

        return router.getActiveClients();
    }

    /**
     * 客户端是否在线
     * @param client
     * @return
     */
    public boolean isClientActive(String client){

        if(null != router){
            RpcContext ctx = router.getClientRpcContext(client);
            if (null != ctx && !ctx.isHeartbeatExpired()) {
                return true;
            }
        }

        return false;
    }


    public String getName(){
        return this.name;
    }

    public RPC_SERVER_STATUS getStatus(){
        return status;
    }

    private void setStatus(RPC_SERVER_STATUS status){
        this.status = status;
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

    public RpcServerMessageRouter getRouter(){
        return this.router;
    }
}
