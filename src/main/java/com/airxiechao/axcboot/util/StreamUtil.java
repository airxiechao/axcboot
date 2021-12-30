package com.airxiechao.axcboot.util;

import java.io.*;
import java.nio.charset.Charset;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class StreamUtil {

    /**
     * 读取输入流
     */
    public static void readStringInputStream(InputStream inputStream, int bufferSize, Charset charset, Consumer<String> consumer) throws Exception {
        try(Reader logReader = new InputStreamReader(inputStream, charset)){
            char[] buffer = new char[bufferSize];
            while(true){
                int num = -1;
                try{
                    num = logReader.read(buffer);
                }catch (IOException e){

                }

                if(num < 0){
                    break;
                }

                StringBuilder sb = new StringBuilder();
                sb.append(buffer, 0, num);
                String str = sb.toString();
                consumer.accept(str);
            }
        }
    }

    public static void readStringInputStream(InputStream inputStream, int bufferSize, Charset charset, Function<String, Boolean> consumer) throws Exception {
        try(Reader logReader = new InputStreamReader(inputStream, charset)){
            char[] buffer = new char[bufferSize];
            while(true){
                int num = -1;
                try{
                    num = logReader.read(buffer);
                }catch (IOException e){

                }
                if(num < 0){
                    break;
                }

                StringBuilder sb = new StringBuilder();
                sb.append(buffer, 0, num);
                String str = sb.toString();

                Boolean ret = consumer.apply(str);
                if(ret != true){
                    break;
                }
            }
        }
    }

    public static void readStringInputStreamNoneBlocking(InputStream inputStream, int bufferSize, Charset charset, Function<String, Boolean> consumer) throws Exception {
        while(true){
            int num = -1;
            byte[] buffer = null;
            try{
                int available = inputStream.available();
                if(available > 0){
                    buffer = inputStream.readNBytes(Math.min(available, bufferSize));
                    num = buffer.length;
                }else{
                    num = 0;
                }
            }catch (IOException e){

            }
            if(num < 0){
                break;
            }

            String str = "";
            if(num > 0) {
                str = new String(buffer, charset);
            }

            Boolean ret = consumer.apply(str);
            if(ret != true){
                break;
            }
        }
    }

    /**
     * 读取输入流到输出流
     * @param inputStream
     * @param bufferSize
     * @param outputStream
     * @throws Exception
     */
    public static void readInputToOutputStream(InputStream inputStream, int bufferSize, OutputStream outputStream) throws Exception {
        byte[] buffer = new byte[bufferSize];
        while(true){
            int num = inputStream.read(buffer);
            if(num < 0){
                break;
            }

            outputStream.write(buffer, 0, num);
        }

        outputStream.flush();
    }

    public static void readInputToOutputStream(InputStream inputStream, int bufferSize, OutputStream outputStream,
                                               BiConsumer<Long, Long> totalAndSpeedConsumer, Supplier<Boolean> stopSupplier) throws Exception {
        long total = 0;
        long speed = 0;

        long num = 0;
        long tick = System.currentTimeMillis();

        byte[] buffer = new byte[bufferSize];
        while(true){
            if(null != stopSupplier && stopSupplier.get()){
                break;
            }

            int nowNum = inputStream.read(buffer);
            if(nowNum < 0){
                break;
            }

            outputStream.write(buffer, 0, nowNum);

            total += nowNum;
            num += nowNum;
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

        outputStream.flush();

        if(null != totalAndSpeedConsumer) {
            totalAndSpeedConsumer.accept(total, speed);
        }

    }

    /**
     * 从输入流读取所有字符串
     * @param inputStream
     * @return
     */
    public static String readString(InputStream inputStream, Charset charset) throws Exception {
        StringBuilder sb = new StringBuilder(512);
        Reader r = new InputStreamReader(inputStream, charset);
        int c;
        while ((c = r.read()) != -1) {
            sb.append((char) c);
        }
        return sb.toString();
    }

    /**
     * 管道流
     */
    public static class PipedStream{
        private PipedOutputStream outputStream = new PipedOutputStream();
        private PipedInputStream inputStream = new PipedInputStream();
        private OutputStreamWriter writer = new OutputStreamWriter(outputStream);
        private InputStreamReader reader = new InputStreamReader(inputStream);

        public PipedStream(){
            try {
                this.outputStream.connect(inputStream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void flush(){
            try {
                this.writer.flush();
                this.outputStream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void close(){
            try {
                this.writer.flush();
                this.outputStream.flush();
                this.outputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public OutputStream getOutputStream() {
            return outputStream;
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        public OutputStreamWriter getWriter() {
            return writer;
        }

        public InputStreamReader getReader() {
            return reader;
        }

        public void write(String text){
            try {
                this.writer.write(text);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
