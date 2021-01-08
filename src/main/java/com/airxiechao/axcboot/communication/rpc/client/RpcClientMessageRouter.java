package com.airxiechao.axcboot.communication.rpc.client;

import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.rpc.common.IRpcMessageHandler;
import com.airxiechao.axcboot.communication.rpc.common.RpcFuture;
import com.airxiechao.axcboot.communication.rpc.common.RpcMessage;
import com.airxiechao.axcboot.communication.rpc.common.RpcContext;
import com.airxiechao.axcboot.communication.rpc.security.IRpcAuthChecker;
import com.airxiechao.axcboot.communication.rpc.util.RpcUtil;
import com.alibaba.fastjson.JSON;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.airxiechao.axcboot.communication.rpc.util.RpcUtil.buildResponseType;

@Sharable
public class RpcClientMessageRouter extends ChannelInboundHandlerAdapter {

    private final static Logger logger = LoggerFactory.getLogger(RpcClientMessageRouter.class);

    private Map<String, IRpcMessageHandler> serviceHandlers;
    private ThreadPoolExecutor executor;
    private RpcContext rpcContext = new RpcContext();
    private Map<String, RpcFuture> pendingRequests = new ConcurrentHashMap<>();
    private RpcClient client;
    private IRpcAuthChecker authChecker;

    public RpcClientMessageRouter(RpcClient client){
        this.client = client;
        this.serviceHandlers = client.getServiceHandlers();
        this.authChecker = client.getAuthChecker();

        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(1000);
        ThreadFactory factory = new ThreadFactory() {

            AtomicInteger seq = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("rpc-client["+client.getName()+"]-" + seq.getAndIncrement());
                t.setDaemon(true);
                return t;
            }

        };

        int numWorkerThreads = client.getNumWorkerThreads();
        this.executor = new ThreadPoolExecutor(numWorkerThreads, numWorkerThreads * 100, 30, TimeUnit.SECONDS,
                queue, factory, new ThreadPoolExecutor.CallerRunsPolicy());

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        updateRpcContext(ctx, new Date());

        client.heartbeatOnce();
        client.runConnectListener();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        closeConection();

        // 尝试重连
        ctx.channel().eventLoop().schedule(() -> {
            client.connect();
        }, client.getReconnectDelaySecs(), TimeUnit.SECONDS);
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

        logger.error("close rpc-client-[{}] to server connection by uncaught error", client.getName(), cause);
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

        logAccess(ctx, message);

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
            future.fail(new Exception("rpc-client-["+client.getName()+"] connection not active error"));
        }
        return future;
    }


    /**
     * 关闭连接
     */
    public void closeConection() {
        pendingRequests.forEach((__, future) -> {
            future.fail(new Exception("rpc-client-["+client.getName()+"] connection not active error"));
        });
        pendingRequests.clear();

        ChannelHandlerContext ctx = rpcContext.getContext();
        if (ctx != null) {
            ctx.close();
        }
        updateRpcContext(null, null);

        client.setStatus(RpcClient.RPC_CLIENT_STATUS.NOT_CONNECTED);
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
