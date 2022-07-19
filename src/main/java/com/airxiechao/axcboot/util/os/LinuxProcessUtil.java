package com.airxiechao.axcboot.util.os;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LinuxProcessUtil {

    public static List<ProcessInfo> listProcess() throws Exception {
        Process process = Runtime.getRuntime().exec("ps axo pid,command");
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));

        List<ProcessInfo> processInfoList = new ArrayList<>();

        String line;
        int no = 0;
        int ic = 0;
        while ((line = br.readLine()) != null) {
            no++;

            if(no == 1){
                ic = line.indexOf("COMMAND");
                continue;
            }

            String strPid = line.substring(0, ic).strip();
            String command = line.substring(ic).strip();
            long pid = Long.valueOf(strPid);
            processInfoList.add(new ProcessInfo(pid, command));
        }

        return processInfoList;
    }
}
