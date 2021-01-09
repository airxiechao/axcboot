package com.airxiechao.axcboot.communication.rpc.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.function.Consumer;

public class StreamUtil {

    /**
     * 读取输入流
     * @param inputStream
     * @param bufferSize
     * @param consumer
     * @throws Exception
     */
    public static void readStringInputStream(InputStream inputStream, int bufferSize, Consumer<String> consumer) throws Exception {
        try(Reader logReader = new InputStreamReader(inputStream, "UTF-8")){
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
}
