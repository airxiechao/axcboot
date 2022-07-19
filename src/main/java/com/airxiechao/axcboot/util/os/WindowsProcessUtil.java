package com.airxiechao.axcboot.util.os;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class WindowsProcessUtil {

    public static List<ProcessInfo> listProcess() throws Exception {
        Process process = Runtime.getRuntime().exec("powershell -command \"Get-CimInstance Win32_Process | Select ProcessId, Name, CommandLine | format-table -wrap\"");
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));

        List<ProcessInfo> processInfoList = new ArrayList<>();
        Long pid = null;
        StringBuilder sb = new StringBuilder();

        String line;
        int in = 0;
        int ic = 0;
        int no = 0;
        while ((line = br.readLine()) != null) {
            no++;

            if(no == 2){
                in = line.indexOf("Name");
                ic = line.indexOf("CommandLine");
            }

            if(no < 4 || line.length() < in){
                continue;
            }

            String strPid = line.substring(0, in - 1).strip();
            if(!strPid.isBlank()){
                if(null != pid){
                    processInfoList.add(new ProcessInfo(pid, sb.toString()));
                }

                String command;
                if(line.length() <= ic || line.substring(ic).isBlank()){
                    command = line.substring(in).strip();
                }else{
                    command = line.substring(ic).strip();
                }

                pid = Long.valueOf(strPid);
                sb.setLength(0);
                sb.append(command);
            }else{
                String command = line.strip();
                sb.append(command);
            }
        }

        if(sb.length() > 0){
            processInfoList.add(new ProcessInfo(pid, sb.toString()));
        }

        return processInfoList;
    }
}
