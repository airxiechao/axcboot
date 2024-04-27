package com.airxiechao.axcboot.util.os;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.airxiechao.axcboot.util.CommandLineUtil.getCommandOutputLines;

public class LinuxProcessUtil {

    public static List<ProcessInfo> listProcess() throws Exception {
        List<String> lines = getCommandOutputLines("ps axo pid,command");

        List<ProcessInfo> processInfoList = new ArrayList<>();

        int no = 0;
        int ic = 0;
        for (String line : lines) {
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

    public static ProcessMetric getProcessMetric(long pid) throws Exception {
        double cpuUsage = getCpuUsage(pid);
        long memoryBytes = getMemoryBytes(pid);

        return new ProcessMetric(pid, cpuUsage, memoryBytes, System.currentTimeMillis());
    }

    private static long getMemoryBytes(Long pid) throws Exception {
        long bytes = 0;
        Pattern vmPattern = Pattern.compile("VmRSS:\\s+(\\d+)\\s+.+");
        List<String> lines = getCommandOutputLines(String.format("cat /proc/%d/status", pid));
        for (String line : lines) {
            Matcher vmMatcher = vmPattern.matcher(line);
            if(vmMatcher.matches()){
                bytes = Long.valueOf(vmMatcher.group(1)) * 1024;
            }
        }

        return bytes;
    }

    private static double getCpuUsage(Long pid) throws Exception {
        String[] stat = getCommandOutputLines(String.format("cat /proc/%d/stat", pid)).get(0).split(" ");
        Long uTime = Long.valueOf(stat[13]);
        Long sTime = Long.valueOf(stat[14]);
        Long startTime = Long.valueOf(stat[21]);
        Double clkTck = Double.valueOf(getCommandOutputLines(String.format("getconf CLK_TCK", pid)).get(0));
        Double upTime = Double.valueOf(getCommandOutputLines(String.format("cat /proc/uptime", pid)).get(0).split(" ")[0]);

        Double uTimeSec = uTime / clkTck;
        Double sTimeSec = sTime / clkTck;
        Double startTimeSec = startTime / clkTck;

        Double elapsedSec = upTime - startTimeSec;
        Double usageSec = uTimeSec + sTimeSec;
        double usage = usageSec / elapsedSec;

        return usage;
    }
}
