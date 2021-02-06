package com.airxiechao.axcboot.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.function.Consumer;

public class StreamUtil {

    /**
     * 读取输入流
     */
    public static void readStringInputStream(InputStream inputStream, int bufferSize, Charset charset, Consumer<String> consumer) throws Exception {
        try(Reader logReader = new InputStreamReader(inputStream, charset)){
            char[] buffer = new char[bufferSize];
            while(true){
                int num = logReader.read(buffer);
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
}
