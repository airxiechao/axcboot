package com.airxiechao.axcboot.communication.rpc.client;

import com.airxiechao.axcboot.communication.common.RequestId;
import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.common.annotation.Query;
import com.airxiechao.axcboot.communication.common.security.IAuthTokenChecker;
import com.airxiechao.axcboot.communication.rpc.common.*;
import com.airxiechao.axcboot.communication.rpc.server.RpcServer;
import com.airxiechao.axcboot.communication.rpc.util.RpcUtil;
import com.airxiechao.axcboot.util.AnnotationUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.channel.ChannelHandler.Sharable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.airxiechao.axcboot.communication.rpc.common.RpcContext.HEARTBEAT_PERIOD_SECS;
import static com.airxiechao.axcboot.communication.rpc.util.RpcUtil.buildResponseType;

/**
 * rpc client
 */
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
    protected IRpcClientListener disconnectListener;

    protected int reconnectDelaySecs = 5;
    private IAuthTokenChecker authTokenChecker;
    private boolean verboseLog = false;
    private SslContext sslCtx;

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
            IAuthTokenChecker authTokenChecker,
            IRpcEventListener connectListener,
            IRpcClientListener disconnectListener
    ){
        this.serverIp = ip;
        this.serverPort = port;
        this.numIoThreads = 1;
        this.numWorkerThreads = numWorkerThreads;
        this.reconnectDelaySecs = reconnectDelaySecs;
        this.authTokenChecker = authTokenChecker;
        this.connectListener = connectListener;
        this.disconnectListener = disconnectListener;

        return this;
    }

    public RpcClient useSsl(
            KeyManager keyManager,
            TrustManager trustManager
    ) throws SSLException {

        sslCtx = SslContextBuilder.forClient()
                .keyManager(keyManager)
                .trustManager(trustManager)
                .build();

        return this;
    }

    public RpcClient useSsl(
            KeyManagerFactory keyManagerFactory,
            TrustManager trustManager
    ) throws SSLException {

        sslCtx = SslContextBuilder.forClient()
                .keyManager(keyManagerFactory)
                .trustManager(trustManager)
                .build();

        return this;
    }

    public RpcClient useSsl(
            File keyCertChainFile,
            File keyFile,
            TrustManager trustManager
    ) throws SSLException {

        this.sslCtx = SslContextBuilder
                .forClient()
                .keyManager(keyCertChainFile, keyFile)
                .trustManager(trustManager)
                .build();

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
        router = new RpcClientMessageRouter();
        clientBootstrap.channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipe = ch.pipeline();

                if(null != sslCtx) {
                    pipe.addLast(sslCtx.newHandler(ch.alloc()));
                }
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

    public void runConnectListener(ChannelHandlerContext ctx){
        if(null != this.connectListener){
            Thread t = new Thread(()-> this.connectListener.handle(ctx));
            t.setDaemon(true);
            t.start();
        }
    }

    public void runDisconnectListener(ChannelHandlerContext ctx, String client){
        if(null != this.disconnectListener){
            Thread t = new Thread(()-> this.disconnectListener.handle(ctx, client));
            t.setDaemon(true);
            t.start();
        }
    }

    /**
     * 注册rpc请求处理器
     * @param handler
     * @param type
     * @param method
     * @return
     */
    public RpcClient registerRpcHandler(Object handler, String type, Method method){
        method.setAccessible(true);
        if(method.getDeclaringClass() != handler.getClass()){
            logger.error("register rpc handler error: method not belong to handler");
            return this;
        }

        serviceHandlers.put(type, (rpcExchange) -> {
            try{
                ChannelHandlerContext ctx = rpcExchange.getCtx();
                Map<String, Object> payload = rpcExchange.getPayload();

                RpcUtil.checkAuth(method, ctx, payload, authTokenChecker);
                RpcUtil.checkParameter(method, payload);
                return (Response)method.invoke(handler, new RpcExchange(ctx, payload));
            }catch (Exception e){
                logger.error("handle rpc service [{}] error",type, e);
                return new Response().error(e.getMessage());
            }

        });

        return this;
    }

    public RpcClient registerRpcHandler(Object handler, Method method){
        method.setAccessible(true);
        Query query = AnnotationUtil.getMethodAnnotation(method, Query.class);
        if (null == query) {
            logger.error("register rpc handler error: no type");
            return this;
        }

        String type = query.value();

        return registerRpcHandler(handler, type, method);
    }

    public RpcClient registerRpcHandler(IRpcMessageHandler handler){
        try{
            Method method = RpcUtil.getHandleMethod(handler.getClass());
            if(null != method){
                registerRpcHandler(handler, method);
            }else{
                logger.error("register rpc handler error: no handle method");
            }

        }catch (Exception e){
            logger.error("register rpc handler error", e);
        }

        return this;
    }

    public RpcClient registerRpcHandler(String type, IRpcMessageHandler handler){
        try{
            Method method = RpcUtil.getHandleMethod(handler.getClass());
            if(null != method){
                registerRpcHandler(handler, type, method);
            }else{
                logger.error("register rpc handler error: no handle method");
            }

        }catch (Exception e){

        }

        return this;
    }

    public RpcClient registerRpcHandler(Class<? extends IRpcMessageHandler> cls){
        try {
            registerRpcHandler(cls.getConstructor().newInstance());
        } catch (Exception e) {
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
        return callServer(type, JSON.toJSONString(payload), null);
    }

    public Response callServer(String type, Map payload, Integer callTimeoutSecs){
        return callServer(type, JSON.toJSONString(payload), callTimeoutSecs);
    }

    private Response callServer(String type, String payload, Integer callTimeoutSecs){

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
            RpcFuture future = router.sendToServer(message);
            if(null != callTimeoutSecs){
                resp = future.get(callTimeoutSecs, TimeUnit.SECONDS);
            }else{
                resp = future.get();
            }

            if(!resp.isSuccess()){
                logger.error("rpc call server type [{}] error: {}", type, resp.getMessage());
            }

            return resp;
        }catch (Exception e) {
            logger.error("rpc call server type [{}] error", type, e);
            throw new RpcException(e);
        }
    }


    /**
     * 服务端是否在线
     * @return
     */
    public boolean isServerActive(){
        RpcContext ctx = router.getRpcContext();
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

    public RpcClient setVerboseLog(boolean verboseLog) {
        this.verboseLog = verboseLog;
        return this;
    }


    /***********************************************************************************************************
     *
     *                                           rpc client router mixin
     *
     ***********************************************************************************************************/



    @Sharable
    public class RpcClientMessageRouter extends ChannelInboundHandlerAdapter {

        private ThreadPoolExecutor executor;
        private RpcContext rpcContext = new RpcContext();
        private Map<String, RpcFuture> pendingRequests = new ConcurrentHashMap<>();
        private boolean shouldReconnect = true;

        public RpcClientMessageRouter(){

            BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(1000);
            ThreadFactory factory = new ThreadFactory() {

                AtomicInteger seq = new AtomicInteger();

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setName("rpc-client["+name+"]-" + seq.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                }

            };

            this.executor = new ThreadPoolExecutor(numWorkerThreads, numWorkerThreads * 100, 30, TimeUnit.SECONDS,
                    queue, factory, new ThreadPoolExecutor.CallerRunsPolicy());

        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            updateRpcContext(ctx, new Date());

            heartbeatOnce();
            runConnectListener(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            closeConection();

            runDisconnectListener(ctx, null);

            // 尝试重连
            if(this.shouldReconnect){
                ctx.channel().eventLoop().schedule(() -> {
                    connect();
                }, reconnectDelaySecs, TimeUnit.SECONDS);
            }
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (!(msg instanceof RpcMessage)) {
                return;
            }

            RpcMessage message = (RpcMessage) msg;

            if(message.isResponse()){
                this.handleResponseMessage(ctx, message);
            }else{
                this.executor.execute(() -> {
                    this.handleServiceMessage(ctx, message);
                });
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.close();

            if(cause.getCause() instanceof SSLHandshakeException){
                this.shouldReconnect = false;
            }

            logger.error("close rpc-client-[{}] to server connection by uncaught error", name, verboseLog ? cause : null);
        }

        /**
         * 处理响应消息
         * @param ctx
         * @param message
         * @throws Exception
         */
        private void handleResponseMessage(ChannelHandlerContext ctx, RpcMessage message) throws Exception {
            RpcFuture future = pendingRequests.remove(message.getRequestId());
            if (future == null) {
                logger.error("rpc future not found with type {}", message.getType());
                return;
            }

            try{
                Response response = JSON.parseObject(message.getPayload(), Response.class);
                future.success(response);
            }catch (Exception e){
                logger.error("parse rpc response message error", e);
                future.fail(e);

                throw new Exception("parse rpc response message error", e);
            }
        }

        /**
         * 处理请求消息
         * @param ctx
         * @param message
         */
        private void handleServiceMessage(ChannelHandlerContext ctx, RpcMessage message){

            if(verboseLog) {
                logAccess(ctx, message);
            }

            IRpcMessageHandler handler = null;
            if(null != message.getType()){
                handler = serviceHandlers.get(message.getType());
            }

            Response response;
            if(null != handler){
                try {
                    String payload = message.getPayload();
                    Map payloadMap = JSON.parseObject(payload, Map.class);
                    response = handler.handle(new RpcExchange(ctx, payloadMap));
                }catch (Exception e){
                    logger.error("handle rpc service [{}] error", message.getType(), e);

                    response = new Response();
                    response.error(e.getMessage());
                }
            }else{
                response = new Response();
                response.error("no rpc service [" + message.getType() + "]");
            }

            ctx.writeAndFlush(new RpcMessage(message.getRequestId(), buildResponseType(message.getType()), JSON.toJSONString(response)));
        }

        /**
         * 向服务端发送rpc消息
         * @param message
         * @return
         */
        public RpcFuture sendToServer(RpcMessage message) {
            ChannelHandlerContext ctx = getActiveChannelHandlerContext();

            RpcFuture future = new RpcFuture();
            ChannelHandlerContext ctx1 = ctx;
            if (ctx != null) {
                ctx.channel().eventLoop().execute(() -> {
                    pendingRequests.put(message.getRequestId(), future);
                    ctx1.writeAndFlush(message);
                });
            } else {
                future.fail(new Exception("rpc-client-["+name+"] connection not active error"));
            }
            return future;
        }


        /**
         * 关闭连接
         */
        public void closeConection() {
            pendingRequests.forEach((__, future) -> {
                future.fail(new Exception("rpc-client-["+name+"] connection not active error"));
            });
            pendingRequests.clear();

            ChannelHandlerContext ctx = rpcContext.getContext();
            if (ctx != null) {
                ctx.close();
            }
            updateRpcContext(null, null);

            setStatus(RpcClient.RPC_CLIENT_STATUS.NOT_CONNECTED);
        }

        /**
         * 关闭
         */
        public void closeGracefully() {
            closeConection();

            this.executor.shutdown();
            try {
                this.executor.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }
            this.executor.shutdownNow();
        }

        public RpcContext getRpcContext(){
            return rpcContext;
        }

        /**
         * 更新rpc上下文
         * @param ctx
         * @param lastHeartbeatTime
         */
        public void updateRpcContext(ChannelHandlerContext ctx, Date lastHeartbeatTime){
            rpcContext.setContext(ctx);
            updateRpcContextLastHeartbeatTime(lastHeartbeatTime);
        }

        public void updateRpcContextLastHeartbeatTime(Date lastHeartbeatTime){
            if(null != lastHeartbeatTime){
                rpcContext.setLastHeartbeatTime(lastHeartbeatTime);
            }
        }

        /**
         * 获取活跃通道上下文
         * @return
         */
        private ChannelHandlerContext getActiveChannelHandlerContext(){
            ChannelHandlerContext ctx = null;
            if(!rpcContext.isHeartbeatExpired()){
                ctx = rpcContext.getContext();
            }

            return ctx;
        }

        /**
         * 记录请求
         * @param ctx
         * @param message
         */
        private void logAccess(ChannelHandlerContext ctx, RpcMessage message){
            String clientIp = RpcUtil.getRemoteIp(ctx);
            logger.info("->> rpc request [{}] from [{}]", message.getType(), clientIp);
        }

    }

}
