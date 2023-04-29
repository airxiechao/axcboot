package com.airxiechao.axcboot.process.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractStateMachine {

    private static final Logger logger = LoggerFactory.getLogger(AbstractStateMachine.class);

    public static final String STATE_INIT = "INIT";

    protected String state = STATE_INIT;
    protected StateTransferMatrix stm = new StateTransferMatrix();

    protected AbstractStateMachine(){
        initStm();
    }

    protected abstract void initStm();
    protected abstract void transferToState(String state);

    public synchronized void enterState(String state){
        if(!checkStateTransfer(this.state, state)){
            logger.error("state machine can not transfer state from [{}] to [{}]", this.state, state);
            throw new RuntimeException("state machine transfer state error");
        }

        this.state = state;

        transferToState(state);
    }

    protected boolean checkStateTransfer(String fromState, String toState){
        return this.stm.hasPath(fromState, toState);
    }

    public String getState() {
        return state;
    }
}
