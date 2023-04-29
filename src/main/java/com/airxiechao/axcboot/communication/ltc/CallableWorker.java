package com.airxiechao.axcboot.communication.ltc;

import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.common.annotation.Param;
import com.airxiechao.axcboot.communication.common.annotation.Params;
import com.airxiechao.axcboot.communication.common.annotation.Query;
import com.airxiechao.axcboot.communication.ltc.queue.CallableFuture;
import com.airxiechao.axcboot.communication.ltc.queue.CallableQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 独立线程执行器
 * 用法：
 * 继承，并在构造函数中调用超类构造函数，然后注册处理器，再启动
 */
public abstract class CallableWorker {

    private static final Logger logger = LoggerFactory.getLogger(CallableWorker.class);

    private String name;
    private CallableQueue callableQueue;
    private Map<String, Method> router = new ConcurrentHashMap<>();

    public CallableWorker(String name, int corePoolSize, int maxPoolSize, int maxQueueSize){
        this.name = name;
        this.callableQueue = new CallableQueue(this.name, corePoolSize, maxPoolSize, maxQueueSize);
    }

    /**
     * 启动
     */
    protected void run(){
        logger.info("worker [{}] run...", name);
        this.callableQueue.eventLoop();
    }

    /**
     * 注册处理器
     * @param cls
     */
    protected void registerHandler(Class<?> cls){
        Method[] methods = cls.getDeclaredMethods();
        for(Method method : methods){
            Query query = method.getAnnotation(Query.class);
            if(null != query){
                String path = query.value();
                router.put(path, method);
            }
        }
    }

    /**
     * 发出异步请求
     * @param query
     * @param params
     * @return
     */
    public Response asyncRequest(String query, Map<String, Object> params){

        Response resp = new Response();

        Method handler = router.get(query);
        if(null == handler){
            logger.error("worker [{}] has no handler for query [{}]", name, query);
            resp.error("no handler");
            return resp;
        }

        try{
            checkParameter(handler, params);
        }catch (Exception e){
            resp.error(e.getMessage());
            return resp;
        }

        callableQueue.offer(()->{
            Object ret = handler.invoke(null, params);
            return ret;
        });

        return resp;
    }

    /**
     * 发出同步请求
     * @param query
     * @param params
     * @return
     */
    public Response request(String query, Map<String, Object> params){

        Response resp = new Response();

        Method handler = router.get(query);
        if(null == handler){
            logger.error("worker [{}] has no handler for query [{}]", name, query);
            resp.error("no handler");
            return resp;
        }

        try{
            checkParameter(handler, params);
        }catch (Exception e){
            resp.error(e.getMessage());
            return resp;
        }

        CallableFuture future = callableQueue.offer(()->{
            Object ret = handler.invoke(null, params);
            return ret;
        });

        try {
            Object ret = future.get();
            resp.success();
            resp.setData(ret);
        } catch (Exception e) {
            logger.error("worker [{}] handle error for query [{}]", name, query, e);
            resp.error();
        }

        return resp;
    }

    /**
     * 检查参数
     * @param method
     * @param queryParams
     * @return
     */
    private void checkParameter(Method method, Map<String, Object> queryParams) throws Exception {
        Annotation[] methodAnnos = method.getAnnotations();
        List<Param> params = new ArrayList<>();
        for(Annotation anno : methodAnnos){
            if(anno instanceof Params){
                params.addAll(Arrays.asList(((Params) anno).value()));
            }else if(anno instanceof Param){
                Param param = (Param)anno;
                params.add(param);
            }
        }

        for(Param param : params){
            String name = param.value();
            boolean required = param.required();


            if(required){
                Object value = queryParams.get(name);

                if(null == value){
                    throw new Exception("parameter [" + name + "] is required");
                }
            }
        }
    }

    public void shutdownNow(){
        callableQueue.shutdownNow();
    }

    public void shutdownGracefully(){
        callableQueue.shutdownGracefully();
    }

    /**
     * 获取名称
     * @return
     */
    public String getName(){
        return this.name;
    }
}