package com.airxiechao.axcboot.process.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class TransactionStep {

    private static final Logger logger = LoggerFactory.getLogger(TransactionStep.class);

    private String stepName;
    private TransactionRunnable forwardRunnable;
    private TransactionRunnable rollbackRunnable;
    private Map stepStore = new HashMap();
    private Map tranStore;
    private Map retStore;

    public TransactionStep(
            String stepName, Map tranStore, Map retStore,
            TransactionRunnable forwardRunnable, TransactionRunnable rollbackRunnable){
        this.stepName = stepName;
        this.tranStore = tranStore;
        this.retStore = retStore;
        this.forwardRunnable = forwardRunnable;
        this.rollbackRunnable = rollbackRunnable;
    }

    public void forward(Logger tlog) throws Exception{
        this.forwardRunnable.run(stepStore, tranStore, retStore, tlog);
    }

    public void rollback(Logger tlog) throws Exception{
        this.rollbackRunnable.run(stepStore, tranStore, retStore, tlog);
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public TransactionRunnable getForwardRunnable() {
        return forwardRunnable;
    }

    public void setForwardRunnable(TransactionRunnable forwardRunnable) {
        this.forwardRunnable = forwardRunnable;
    }

    public TransactionRunnable getRollbackRunnable() {
        return rollbackRunnable;
    }

    public void setRollbackRunnable(TransactionRunnable rollbackRunnable) {
        this.rollbackRunnable = rollbackRunnable;
    }
}
