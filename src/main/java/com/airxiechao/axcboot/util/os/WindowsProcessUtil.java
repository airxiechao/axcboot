package com.airxiechao.axcboot.util.os;

import com.airxiechao.axcboot.util.StringUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static com.airxiechao.axcboot.util.CommandLineUtil.getCommandOutputLines;

public class WindowsProcessUtil {

    public static List<ProcessInfo> listProcess() throws Exception {
        List<String> lines = getCommandOutputLines("powershell -command \"Get-CimInstance Win32_Process | Select ProcessId, Name, CommandLine | format-table -wrap\"");

        List<ProcessInfo> processInfoList = new ArrayList<>();
        Long pid = null;
        StringBuilder sb = new StringBuilder();

        int in = 0;
        int ic = 0;
        int no = 0;
        for (String line : lines) {
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

    public static ProcessMetric getProcessMetric(long pid) throws Exception {
        List<String> lines = getCommandOutputLines(String.format("powershell -command \"Get-WmiObject Win32_PerfFormattedData_PerfProc_Process -Filter \"IDProcess=%d\"\"", pid));

        Double cpuUsage = null;
        Long memoryBytes = null;

        for (String line : lines) {
            if(line.startsWith("PercentProcessorTime")){
                String strCpuUsage = line.substring(line.indexOf(":")+1).strip();
                if(!StringUtil.isBlank(strCpuUsage)){
                    cpuUsage = Long.valueOf(strCpuUsage) / 100.0;
                }
            }else if(line.startsWith("WorkingSetPrivate")){
                String strMemoryBytes = line.substring(line.indexOf(":")+1).strip();
                if(!StringUtil.isBlank(strMemoryBytes)){
                    memoryBytes = Long.valueOf(strMemoryBytes);
                }
            }
        }

        return new ProcessMetric(pid, cpuUsage, memoryBytes, System.currentTimeMillis());
    }
}
