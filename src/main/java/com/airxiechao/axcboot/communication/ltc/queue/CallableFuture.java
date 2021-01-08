package com.airxiechao.axcboot.communication.ltc.queue;

import java.util.concurrent.*;

public class CallableFuture implements Future {

    private Object result;
    private Throwable error;
    private CountDownLatch latch = new CountDownLatch(1);

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return result != null || error != null;
    }

    public void success(Object result) {
        this.result = result;
        latch.countDown();
    }

    public void fail(Throwable error) {
        this.error = error;
        latch.countDown();
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        latch.await();
        if (error != null) {
            throw new ExecutionException(error);
        }
        return result;
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException {
        latch.await(timeout, unit);
        if (error != null) {
            throw new ExecutionException(error);
        }
        return result;
    }

}