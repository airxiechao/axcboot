package com.airxiechao.axcboot.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CommandLineUtil {
    public static List<String> getCommandOutputLines(String command) throws Exception {
        List<String> lines = new ArrayList<>();
        Process process = Runtime.getRuntime().exec(command);
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))){
            String line;
            while((line = reader.readLine()) != null){
                lines.add(line);
            }
        }

        return lines;
    }
}
