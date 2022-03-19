package com.airxiechao.axcboot.communication.ltc.queue;

import com.airxiechao.axcboot.process.threadpool.ThreadPool;
import com.airxiechao.axcboot.process.threadpool.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

public class CallableQueue {

    private final static Logger logger = LoggerFactory.getLogger(CallableQueue.class);

    private BlockingDeque<ICallableEvent> eventQueue = new LinkedBlockingDeque<>();
    private Map<ICallableEvent, CallableFuture> pendingEvents = new ConcurrentHashMap<>();
    private String name;
    private ThreadPool threadPool;

    public CallableQueue(String name, int corePoolSize, int maxPoolSize, int maxQueueSize){
        this.name = name;
        this.threadPool = ThreadPoolManager.getInstance().getThreadPool(this.name, corePoolSize, maxPoolSize, maxQueueSize);
    }

    public CallableFuture offer(ICallableEvent event){
        CallableFuture future = new CallableFuture();
        pendingEvents.put(event, future);
        boolean ret = eventQueue.offer(event);

        if(ret){
            return future;
        }else{
            pendingEvents.remove(event);
            logger.info("event loop [{}] offer error", name);
            return null;
        }
    }

    public void eventLoop(){
        threadPool.getExecutor().execute(()->{
            while(true){
                try {
                    ICallableEvent event = eventQueue.take();
                    CallableFuture future = pendingEvents.get(event);
                    if(future != null){
                        try{
                            logger.info("event pool [{}] execute [active:{}, queue:{}]...",
                                    name, threadPool.getExecutor().getActiveCount(), threadPool.getExecutor().getQueue().size());
                            threadPool.getExecutor().execute(()->{
                                try {
                                    Object ret = event.handle();
                                    future.success(ret);
                                } catch (Exception e) {
                                    logger.error("event loop [{}] execute error", name, e);
                                    future.fail(e);
                                }
                            });
                        }catch (Exception e){
                            logger.error("event loop [{}] execute error", name, e);
                            future.fail(e);
                        }finally {
                            pendingEvents.remove(event);
                        }
                    }else{
                        logger.info("event loop [{}] no such future", name);
                    }

                } catch (Exception e) {
                    logger.error("event loop [{}] error", name, e);
                }
            }
        });

    }

    public void shutdownNow(){
        threadPool.shutdownNow();
    }

    public void shutdownGracefully(){
        threadPool.shutdownGracefully();

    }
}