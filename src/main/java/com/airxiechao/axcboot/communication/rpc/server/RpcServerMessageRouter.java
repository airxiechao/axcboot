package com.airxiechao.axcboot.communication.rpc.server;

import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.rpc.common.*;
import com.airxiechao.axcboot.communication.rpc.security.IRpcAuthChecker;
import com.airxiechao.axcboot.communication.rpc.util.RpcUtil;
import com.airxiechao.axcboot.util.UuidUtil;
import com.alibaba.fastjson.JSON;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.airxiechao.axcboot.communication.rpc.util.RpcUtil.buildResponseType;

@Sharable
public class RpcServerMessageRouter extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(RpcServerMessageRouter.class);

    private Map<String, IRpcMessageHandler> serviceHandlers;
    private ThreadPoolExecutor executor;
    private Map<String, RpcContext> rpcContexts = new ConcurrentHashMap<>();
    private Map<String, RpcClientFuture> pendingRequests = new ConcurrentHashMap<>();
    private RpcServer rpcServer;
    private IRpcAuthChecker authChecker;

    public RpcServerMessageRouter(RpcServer rpcServer){
        this.rpcServer = rpcServer;
        this.serviceHandlers = rpcServer.getServiceHandlers();
        this.authChecker = rpcServer.getAuthChecker();

        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(1000);
        ThreadFactory factory = new ThreadFactory() {

            AtomicInteger seq = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("rpc-server-["+rpcServer.getName()+"]-" + seq.getAndIncrement());
                t.setDaemon(true);
                return t;
            }

        };

        int numWorkerThreads = rpcServer.getNumWorkerThreads();
        this.executor = new ThreadPoolExecutor(numWorkerThreads, numWorkerThreads * 100, 30, TimeUnit.SECONDS,
                queue, factory, new ThreadPoolExecutor.CallerRunsPolicy());
    }



    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        String clientName = UuidUtil.random();

        logger.info("rpc-client-[{}] active", clientName);

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
        logger.error("close rpc-client-[{}] connection by uncaught error", client,
                this.rpcServer.isVerboseLog() ? cause : null);
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

        if(this.rpcServer.isVerboseLog()){
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

                RpcUtil.checkAuth(handler, ctx, payloadMap, authChecker);
                RpcUtil.checkParameter(handler, payloadMap);
                response = handler.handle(ctx, payloadMap);
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

        this.rpcContexts.put(client, rpcContext);
    }

    /**
     * 获取客户端rpc上下文
     * @param client
     * @return
     */
    public RpcContext getClientRpcContext(String client){
        RpcContext rpcContext = this.rpcContexts.get(client);
        return rpcContext;
    }

    /**
     * 删除客户端rpc上下文
     * @param client
     */
    private void removeClientRpcContext(String client){
        if(this.rpcContexts.containsKey(client)){
            this.rpcContexts.remove(client);
        }
    }

    /**
     * 获取在线客户端
     * @return
     */
    public List<String> getActiveClients(){
        List<String> clients = new ArrayList<>();
        for(Map.Entry<String, RpcContext> entry : rpcContexts.entrySet()){
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
        Optional<String> opt = rpcContexts.entrySet().stream()
                .filter(c->ctx.equals(c.getValue().getContext()))
                .map(c->c.getKey())
                .findFirst();

        String client = null;
        if(opt.isPresent()) {
            client = opt.get();
        }

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
