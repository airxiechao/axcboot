package com.airxiechao.axcboot.process.transaction;

import com.airxiechao.axcboot.communication.common.Response;
import com.airxiechao.axcboot.util.StringUtil;
import com.airxiechao.axcboot.util.logger.TraceLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TransactionPipeline {

    private static final Logger logger = LoggerFactory.getLogger(TransactionPipeline.class);

    private List<TransactionStep> steps = new ArrayList<>();

    private String uuid;
    private String name;
    private Map tranStore = new HashMap();
    private Map retStore = new HashMap();
    private Map<String, Boolean> stepStatus = new HashMap();

    public TransactionPipeline(String name){
        this.name = name;
        this.uuid = UUID.randomUUID().toString();
    }

    public TransactionPipeline addStep(
            String stepName,
            TransactionRunnable forwardRunnable,
            TransactionRunnable rollbackRunnable){
        stepName = (steps.size()+1) + "_" + stepName;
        TransactionStep step = new TransactionStep(stepName, tranStore, retStore, forwardRunnable, rollbackRunnable);
        steps.add(step);

        return this;
    }

    public Response<Map> execute(){
        Logger tlog = TraceLogger.wrap(logger);
        tlog.info("transaction [{}, {}] begin...", name, uuid);
        Stack<TransactionStep> stack = new Stack<>();

        // clear transactional status
        stepStatus.clear();
        for(TransactionStep step : steps){
            stepStatus.put(step.getStepName(), false);
        }

        boolean success = false;
        String currentStepName = "";
        Exception exception = null;
        try{
            for(TransactionStep step : steps){
                currentStepName = step.getStepName();
                tlog.info("transaction [{}, {}] step {} forward...", name, uuid, currentStepName);
                step.forward(tlog);
                stack.push(step);
                stepStatus.put(currentStepName, true);
            }

            success = true;
        }catch (Exception e){
            tlog.error("transaction [{}, {}] step {} forward error.", name, uuid, currentStepName, e);
            exception = e;

            while(!stack.empty()){
                TransactionStep step = stack.pop();
                tlog.info("transaction [{}, {}] step {} rollback...", name, uuid, step.getStepName());

                try{
                    step.rollback(tlog);
                }catch (Exception e2){
                    logger.error("{} rollback error.", step.getStepName(), e2);
                }

            }
        }

        tlog.info("transaction [{}, {}] finish [{}].", name, uuid, success);

        Response<Map> resp = new Response();
        if(success){
            resp.success();
            resp.setData(retStore);
        }else{
            String errMessage = exception.getMessage();
            if(StringUtil.isEmpty(errMessage)){
                errMessage = exception.getCause().getMessage();
            }
            resp.error(errMessage);
            resp.setData(retStore);
        }

        return resp;
    }

    public Map<String, Boolean> getStepStatus(){
        return stepStatus;
    }

    public Map getTranStore() {
        return tranStore;
    }
}
