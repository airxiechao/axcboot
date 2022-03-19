package com.airxiechao.axcboot.util.http;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

import java.io.File;
import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class ProgressFileRequestBody extends RequestBody {

    private static final int SEGMENT_SIZE = 2048; // okio.Segment.SIZE

    private File file;
    private String contentType;
    private BiConsumer<Long, Long> totalAndSpeedConsumer;
    private Supplier<Boolean> stopSupplier;

    public ProgressFileRequestBody(File file, String contentType, BiConsumer<Long, Long> totalAndSpeedConsumer, Supplier<Boolean> stopSupplier) {
        this.file = file;
        this.contentType = contentType;
        this.totalAndSpeedConsumer = totalAndSpeedConsumer;
        this.stopSupplier = stopSupplier;
    }

    @Override
    public long contentLength() {
        return file.length();
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse(contentType);
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        Source source = null;
        try {
            source = Okio.source(file);

            long total = 0;
            long speed = 0;

            long num = 0;
            long tick = System.currentTimeMillis();

            while (true) {
                if(null != stopSupplier && stopSupplier.get()){
                    break;
                }

                long read = source.read(sink.buffer(), SEGMENT_SIZE);
                if(read < 0){
                    break;
                }

                total += read;
                num += read;
                sink.flush();

                long nowTick = System.currentTimeMillis();
                if (nowTick >= tick + 1000) {

                    long diffTick = nowTick - tick;
                    speed = num * 1000 / diffTick;

                    if(null != totalAndSpeedConsumer) {
                        totalAndSpeedConsumer.accept(total, speed);
                    }

                    num = 0;
                    tick = nowTick;
                }
            }

            if(null != totalAndSpeedConsumer) {
                totalAndSpeedConsumer.accept(total, speed);
            }

        } finally {
            Util.closeQuietly(source);
        }
    }

}