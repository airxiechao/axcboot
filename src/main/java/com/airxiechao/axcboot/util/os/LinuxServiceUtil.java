package com.airxiechao.axcboot.util.os;

import com.airxiechao.axcboot.util.StringUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static com.airxiechao.axcboot.util.CommandLineUtil.getCommandOutputLines;

public class LinuxServiceUtil {

    public static List<ServiceInfo> listService() throws Exception {
        List<String> lines = getCommandOutputLines("systemctl list-units --no-pager --no-legend --all");

        List<ServiceInfo> serviceInfoList = new ArrayList<>();

        int no = 0;
        for (String line : lines) {
            no++;

            String[] tokens = line.split("\\s+");
            String name = tokens[0].strip();
            boolean running = "active".equals(tokens[2].strip());
            serviceInfoList.add(new ServiceInfo(running, name));
        }

        return serviceInfoList;
    }

    public static Long getServicePid(String serviceName) throws Exception {
        List<String> lines = getCommandOutputLines(String.format("systemctl status %s | grep PID", serviceName));

        String strPid = null;
        for (String line : lines) {
            line = line.strip();
            if(line.startsWith("Main PID")){
                String[] tokens = line.substring(line.indexOf(":")+1).strip().split("\\s+");
                strPid = tokens[0].strip();
            }
        }

        if(StringUtil.isBlank(strPid)){
            return null;
        }else{
            return Long.valueOf(strPid);
        }
    }
}
