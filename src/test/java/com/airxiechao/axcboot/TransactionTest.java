package com.airxiechao.axcboot;

import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.process.transaction.TransactionPipeline;

import java.util.Map;

public class TransactionTest {

    public static void main(String[] args){
        TransactionPipeline pipeline = new TransactionPipeline("test-pipeline");

        Map map = pipeline.getTranStore();
        map.put("a", 1);

        pipeline.addStep("step-1", (stepStore, tranStore, retStore, tlog)->{
            tlog.info("add 1");

            Integer a = (Integer) tranStore.get("a");
            tranStore.put("a", a+1);
        }, (stepStore, tranStore, retStore, tlog)->{
            tlog.info("rollback");

            Integer a = (Integer) tranStore.get("a");
            tranStore.put("a", a-1);
        });

        pipeline.addStep("step-2", (stepStore, tranStore, retStore, tlog)->{
            tlog.info("add 2");

            Integer a = (Integer) tranStore.get("a");
            tranStore.put("a", a+2);

            retStore.put("ret", a+2);
        }, (stepStore, tranStore, retStore, tlog)->{
            tlog.info("rollback");

            Integer a = (Integer) tranStore.get("a");
            tranStore.put("a", a-2);
        });

        Response resp = pipeline.execute();
        Map ret = (Map)resp.getData();
        System.out.println(ret.get("ret"));
    }
}
