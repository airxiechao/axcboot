package com.airxiechao.axcboot.communication.rpc.server;

import com.airxiechao.axcboot.communication.common.RequestId;
import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.common.annotation.Query;
import com.airxiechao.axcboot.communication.common.security.IAuthTokenChecker;
import com.airxiechao.axcboot.communication.rpc.common.*;
import com.airxiechao.axcboot.communication.rpc.util.RpcUtil;
import com.airxiechao.axcboot.util.AnnotationUtil;
import com.airxiechao.axcboot.util.UuidUtil;
import com.alibaba.fastjson.JSON;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.channel.ChannelHandler.Sharable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.airxiechao.axcboot.communication.rpc.util.RpcUtil.buildResponseType;

/**
 * rpc server
 */
public class RpcServer {
    private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);

    public enum RPC_SERVER_STATUS {
        NOT_STARTED, STARTED, STOPPED
    }

    private String name;
    private String uuid;
    private String serverIp;
    private int serverPort;
    private int numIoThreads;
    private int numWorkerThreads;
    private ServerBootstrap serverBootstrap;
    private EventLoopGroup serverGroup;
    private Channel serverChannel;
    private Map<String, IRpcMessageHandler> serviceHandlers = new HashMap<>();
    private RpcServerMessageRouter router;
    private IAuthTokenChecker authTokenChecker;
    private boolean verboseLog = false;
    private SslContext sslCtx;
    protected IRpcClientListener connectListener;
    protected IRpcClientListener disconnectListener;

    private RPC_SERVER_STATUS status = RPC_SERVER_STATUS.NOT_STARTED;

    public RpcServer(String name){
        this.name = name;
        this.uuid = UuidUtil.random();
    }

    public RpcServer(String name, String uuid){
        this.name = name;
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public RpcServer config(
            String ip,
            int port,
            int numIoThreads,
            int numWorkerThreads,
            IAuthTokenChecker authChecker,
            IRpcClientListener connectListener,
            IRpcClientListener disconnectListener
    ){
        this.serverIp = ip;
        this.serverPort = port;
        this.numIoThreads = numIoThreads;
        this.numWorkerThreads = numWorkerThreads;
        this.authTokenChecker = authChecker;
        this.connectListener = connectListener;
        this.disconnectListener = disconnectListener;

        registerHeartbeatHandler();

        return this;
    }

    public RpcServer useSsl(
            KeyManager keyManager,
            TrustManager trustManager
    ) throws SSLException {

        this.sslCtx = SslContextBuilder
                .forServer(keyManager)
                .trustManager(trustManager)
                .clientAuth(ClientAuth.REQUIRE)
                .build();

        return this;
    }

    public RpcServer useSsl(
            KeyManagerFactory keyManagerFactory,
            TrustManager trustManager
    ) throws SSLException {

        this.sslCtx = SslContextBuilder
                .forServer(keyManagerFactory)
                .trustManager(trustManager)
                .clientAuth(ClientAuth.REQUIRE)
                .build();

        return this;
    }

    public RpcServer useSsl(
            File keyCertChainFile,
            File keyFile,
            TrustManager trustManager
    ) throws SSLException {

        this.sslCtx = SslContextBuilder
                .forServer(keyCertChainFile, keyFile)
                .trustManager(trustManager)
                .clientAuth(ClientAuth.REQUIRE)
                .build();

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
        router = new RpcServerMessageRouter();
        serverBootstrap.channel(NioServerSocketChannel.class).childHandler(
                new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipe = socketChannel.pipeline();

                        if(null != sslCtx) {
                            pipe.addLast(sslCtx.newHandler(socketChannel.alloc()));
                        }
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
     * @param handler
     * @param type
     * @param method
     * @return
     */
    public RpcServer registerRpcHandler(Object handler, String type, Method method){
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

    public RpcServer registerRpcHandler(Object handler, Method method){
        method.setAccessible(true);
        Query query = AnnotationUtil.getMethodAnnotation(method, Query.class);
        if (null == query) {
            logger.error("register rpc handler error: no type");
            return this;
        }

        String type = query.value();

        return registerRpcHandler(handler, type, method);
    }

    public RpcServer registerRpcHandler(IRpcMessageHandler handler){
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

    public RpcServer registerRpcHandler(String type, IRpcMessageHandler handler){
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

    public RpcServer registerRpcHandler(Class<? extends IRpcMessageHandler> cls){
        try {
            registerRpcHandler(cls.getConstructor().newInstance());
        } catch (Exception e) {
            logger.error("register rpc handler error", e);
        }

        return this;
    }

    /**
     * 注册心跳处理
     */
    private void registerHeartbeatHandler(){
        registerRpcHandler(RpcMessage.TYPE_PING, (rpcExchange) -> {
            ChannelHandlerContext ctx = rpcExchange.getCtx();
            Map<String, Object> payload = rpcExchange.getPayload();

            String clientName = router.getClientByContext(ctx);
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
        return callClient(client, type, JSON.toJSONString(payload), null);
    }

    public Response callClient(String client, String type, Map payload, Integer callTimeoutSecs){
        return callClient(client, type, JSON.toJSONString(payload), callTimeoutSecs);
    }

    private Response callClient(String client, String type, String payload, Integer callTimeoutSecs){

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
            RpcFuture future = router.sendToClient(client, message);
            if(null != callTimeoutSecs){
                resp = future.get(callTimeoutSecs, TimeUnit.SECONDS);
            }else{
                resp = future.get();
            }

            if(!resp.isSuccess()){
                logger.error("rpc call client [{}] type [{}] error: {}", client, type, resp.getMessage());
            }

            return resp;
        }catch (Exception e) {
            logger.error("rpc call client [{}] type [{}] error", client, type, e);
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

    public RPC_SERVER_STATUS getStatus(){
        return status;
    }

    private void setStatus(RPC_SERVER_STATUS status){
        this.status = status;
    }

    public RpcServerMessageRouter getRouter(){
        return router;
    }

    public RpcServer setVerboseLog(boolean verboseLog) {
        this.verboseLog = verboseLog;
        return this;
    }



    /**************************************************************************************************************
     *
     *                                          rpc server router mixin
     *
     **************************************************************************************************************/



    @Sharable
    public class RpcServerMessageRouter extends ChannelInboundHandlerAdapter {

        private ThreadPoolExecutor executor;
        private RpcContextContainer rpcContextContainer = new RpcContextContainer();
        private Map<String, RpcClientFuture> pendingRequests = new ConcurrentHashMap<>();

        public RpcServerMessageRouter(){

            BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(1000);
            ThreadFactory factory = new ThreadFactory() {

                AtomicInteger seq = new AtomicInteger();

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setName("rpc-server-["+name+"]-" + seq.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                }

            };

            this.executor = new ThreadPoolExecutor(numWorkerThreads, numWorkerThreads * 100, 30, TimeUnit.SECONDS,
                    queue, factory, new ThreadPoolExecutor.CallerRunsPolicy());
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {

            String clientName = UuidUtil.random();

            logger.info("rpc-client-[{}] active", clientName);

            runConnectListener(ctx, clientName);

            // 如果已经存在相同名称的其他客户端，则断开连接
            updateOrCloseRpcContext(clientName, ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {

            String client = getClientByContext(ctx);

            logger.info("rpc-client-[{}] inactive", client);

            if(null != client){
                clearClient(client);
            }

            runDisconnectListener(ctx, client);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception{
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

            String client = getClientByContext(ctx);
            logger.error("close rpc-client-[{}] connection by uncaught error", client, verboseLog ? cause : null);
        }

        public void runConnectListener(ChannelHandlerContext ctx, String client){
            if(null != connectListener){
                Thread t = new Thread(()-> connectListener.handle(ctx, client));
                t.setDaemon(true);
                t.start();
            }
        }

        public void runDisconnectListener(ChannelHandlerContext ctx, String client){
            if(null != disconnectListener){
                Thread t = new Thread(()-> disconnectListener.handle(ctx, client));
                t.setDaemon(true);
                t.start();
            }
        }

        /**
         * 处理响应消息
         * @param ctx
         * @param message
         */
        private void handleResponseMessage(ChannelHandlerContext ctx, RpcMessage message) throws Exception {
            RpcClientFuture clientFuture = pendingRequests.remove(message.getRequestId());
            if (clientFuture == null) {
                logger.error("rpc future not found with type {}", message.getType());
                return;
            }

            try{
                Response response = JSON.parseObject(message.getPayload(), Response.class);
                clientFuture.success(response);
            }catch (Exception e){
                logger.error("parse rpc response message error", e);
                clientFuture.fail(e);

                throw new Exception("parse rpc response message error", e);
            }
        }

        /**
         * 处理请求消息
         * @param ctx
         * @param message
         */
        private void handleServiceMessage(ChannelHandlerContext ctx, RpcMessage message){

            if(verboseLog){
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
         * 向客户端发送rpc消息
         * @param client
         * @param message
         * @return
         */
        public RpcFuture sendToClient(String client, RpcMessage message) {

            ChannelHandlerContext ctx = null;
            RpcContext rpcContext = getClientRpcContext(client);
            if(null != rpcContext){
                if(!rpcContext.isHeartbeatExpired()){
                    ctx = rpcContext.getContext();
                }
            }

            RpcClientFuture clientFuture = new RpcClientFuture();
            ChannelHandlerContext ctx1 = ctx;
            if (ctx1 != null) {

                clientFuture.setClientName(client);
                ctx1.channel().eventLoop().execute(() -> {
                    pendingRequests.put(message.getRequestId(), clientFuture);
                    ctx1.writeAndFlush(message);
                });
            } else {
                clientFuture.fail(new Exception("rpc-client-["+client+"] connection not active"));
            }
            return clientFuture;
        }

        /**
         * 关闭
         */
        public void closeGracefully() {
            this.executor.shutdown();
            try {
                this.executor.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }
            this.executor.shutdownNow();
        }

        /**
         * 清理客户端
         * @param client
         */
        private void clearClient(String client){
            List<String> futureKeys = pendingRequests.entrySet().stream()
                    .filter(c->client.equals(c.getValue().getClientName()))
                    .map(c->c.getKey())
                    .collect(Collectors.toList());

            for(String key : futureKeys){
                RpcClientFuture future = pendingRequests.get(key);
                future.fail(new Exception("rpc-client-["+client+"] connection not active error"));
                pendingRequests.remove(key);
            }

            updateRpcContext(client, null, null);
        }


        /**
         * 更新rpc上下文
         * @param client
         * @param ctx
         * @param date
         */
        public void updateRpcContext(String client, ChannelHandlerContext ctx, Date date){

            if(null == ctx && null == date){
                removeClientRpcContext(client);
                return;
            }

            RpcContext rpcContext = getClientRpcContext(client);
            if(null == rpcContext){
                rpcContext = new RpcContext();
            }

            rpcContext.setContext(ctx);
            if(null != date){
                rpcContext.setLastHeartbeatTime(date);
            }

            this.rpcContextContainer.put(client, rpcContext);
        }

        /**
         * 获取客户端rpc上下文
         * @param client
         * @return
         */
        public RpcContext getClientRpcContext(String client){
            RpcContext rpcContext = this.rpcContextContainer.getRpcContextByClient(client);
            return rpcContext;
        }

        /**
         * 删除客户端rpc上下文
         * @param client
         */
        private void removeClientRpcContext(String client){
            if(this.rpcContextContainer.containsClient(client)){
                this.rpcContextContainer.removeByClient(client);
            }
        }

        /**
         * 获取在线客户端
         * @return
         */
        public List<String> getActiveClients(){
            List<String> clients = new ArrayList<>();
            for(Map.Entry<String, RpcContext> entry : rpcContextContainer.clientRpcContextEntrySet()){
                String name = entry.getKey();
                RpcContext context = entry.getValue();

                if(!context.isHeartbeatExpired()){
                    clients.add(name);
                }
            }
            return clients;
        }

        /**
         * 获取客户端
         * @param ctx
         * @return
         */
        public String getClientByContext(ChannelHandlerContext ctx){
            String client = rpcContextContainer.getClientByChannelContext(ctx);
            return client;
        }

        /**
         * 查是否已经存在这个名称的其他客户端，并且没有过期
         * @param ctx
         * @param clientName
         * @return
         */
        public boolean hasOtherClientWithName(ChannelHandlerContext ctx, String clientName){
            if(null != clientName){
                RpcContext rpcContext = getClientRpcContext(clientName);
                if(null != rpcContext && !rpcContext.isHeartbeatExpired()){
                    ChannelHandlerContext channelHandlerContext = rpcContext.getContext();
                    if(null != channelHandlerContext){
                        if(!ctx.equals(channelHandlerContext)){
                            return true;
                        }
                    }
                }
            }

            return false;
        }

        /**
         * 更新rpc上下文，如果已经存在相同名称的其他客户端，则断开连接
         * @param clientName
         * @param ctx
         */
        public void updateOrCloseRpcContext(String clientName, ChannelHandlerContext ctx) throws Exception {
            boolean has = hasOtherClientWithName(ctx, clientName);
            if(has){
                ctx.close();
                logger.error("client [{}] already exists", clientName);
                throw new Exception("client ["+clientName+"] already exists");
            }

            updateRpcContext(clientName, ctx, new Date());
        }

        /**
         * 记录请求
         * @param ctx
         * @param message
         */
        private void logAccess(ChannelHandlerContext ctx, RpcMessage message){
            String client = getClientByContext(ctx);
            String clientIp = RpcUtil.getRemoteIp(ctx);
            logger.info("->> rpc request [{}] from [client:{}, ip:{}]", message.getType(), client, clientIp);
        }
    }

}
