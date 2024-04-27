package com.airxiechao.axcboot.process.transaction;

import org.slf4j.Logger;

import java.util.Map;

public interface TransactionRunnable {

    void run(Map stepStore, Map tranStore, Map retStore, Logger tlog) throws Exception;
}
