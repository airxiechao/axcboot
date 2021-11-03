package com.airxiechao.axcboot.util;

import java.io.*;
import java.nio.charset.Charset;
import java.util.function.Consumer;
import java.util.function.Function;

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
