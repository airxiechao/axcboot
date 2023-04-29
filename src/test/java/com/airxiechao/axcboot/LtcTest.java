package com.airxiechao.axcboot;

import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.communication.ltc.CallableWorker;
import com.airxiechao.axcboot.communication.ltc.WorkerManager;
import com.airxiechao.axcboot.communication.common.annotation.Query;

import java.util.HashMap;
import java.util.Map;


public class LtcTest {

    public static class TestWorker extends CallableWorker{

        public TestWorker() {
            super("test-worker", 10, 100, 100);

            registerHandler(TestWorkerHandler.class);

            run();
        }
    }

    public static class TestWorkerHandler {

        @Query("add")
        public static Integer add(Map params){
            Integer a = (Integer)params.get("a");
            Integer b = (Integer)params.get("b");

            return a+b;
        }
    }

    public static void main(String[] args){
        CallableWorker worker = WorkerManager.getInstance().getWorker(TestWorker.class);

        Map params = new HashMap();
        params.put("a", 1);
        params.put("b", 2);
        Response resp = worker.request("add", params);
        System.out.println(resp.getData());
    }
}


