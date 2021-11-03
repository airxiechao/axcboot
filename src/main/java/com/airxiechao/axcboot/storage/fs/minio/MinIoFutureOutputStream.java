package com.airxiechao.axcboot.storage.fs.minio;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Future;

public class MinIoFutureOutputStream extends OutputStream {

    private final OutputStream wrapped;
    private final Future<?> future;

    public MinIoFutureOutputStream(OutputStream outputStream, Future<?> future) {
        wrapped = outputStream;
        this.future = future;
    }

    @Override
    public void write(int b) throws IOException {
        wrapped.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        wrapped.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        wrapped.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        wrapped.flush();
    }

    @Override
    public void close() throws IOException {
        wrapped.close();
        try {
            future.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}