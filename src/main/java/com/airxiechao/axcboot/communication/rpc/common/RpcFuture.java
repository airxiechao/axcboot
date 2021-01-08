package com.airxiechao.axcboot.communication.rpc.common;

import com.airxiechao.axcboot.communication.common.Response;

import java.sql.Time;
import java.util.concurrent.*;

public class RpcFuture implements Future<Response> {

    private Response result;
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

    public void success(Response result) {
        this.result = result;
        latch.countDown();
    }

    public void fail(Throwable error) {
        this.error = error;
        latch.countDown();
    }

    @Override
    public Response get() throws InterruptedException, ExecutionException {
        latch.await();
        if (null == result || null != error) {
            throw new ExecutionException(error);
        }
        return result;
    }

    @Override
    public Response get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean got = latch.await(timeout, unit);
        if(!got){
            throw new TimeoutException();
        }
        if (null == result || null != error) {
            throw new ExecutionException(error);
        }
        return result;
    }

}
