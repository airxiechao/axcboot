package com.airxiechao.axcboot.communication.ltc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WorkerManager {

    private static final Logger logger = LoggerFactory.getLogger(WorkerManager.class);

    private static WorkerManager ourInstance = new WorkerManager();

    private Map<Class<? extends CallableWorker>, CallableWorker> workers = new ConcurrentHashMap<>();

    public static WorkerManager getInstance() {
        return ourInstance;
    }

    private WorkerManager() {
    }

    public CallableWorker getWorker(Class<? extends CallableWorker> cls){
        CallableWorker worker = workers.get(cls);
        if(null == worker){
            try {
                worker = cls.getConstructor().newInstance();
                workers.put(cls, worker);
            } catch (Exception e) {
                logger.error("worker construct error", e);
            }
        }

        return worker;
    }
}