package com.airxiechao.axcboot.util.os;

import com.airxiechao.axcboot.util.StringUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static com.airxiechao.axcboot.util.CommandLineUtil.getCommandOutputLines;

public class WindowsServiceUtil {

    public static List<ServiceInfo> listService() throws Exception {
        List<String> lines = getCommandOutputLines("powershell -command \"Get-Service | Select Status, Name | format-table -wrap\"");

        List<ServiceInfo> serviceInfoList = new ArrayList<>();
        Boolean running = null;
        StringBuilder sb = new StringBuilder();

        int in = 0;
        int no = 0;
        for (String line : lines) {
            no++;

            if(no == 2){
                in = line.indexOf("Name");
            }

            if(no < 4 || line.length() < in){
                continue;
            }

            String strStatus = line.substring(0, in - 1).strip();
            if(!strStatus.isBlank()){
                if(null != running){
                    serviceInfoList.add(new ServiceInfo(running, sb.toString()));
                }

                String name = line.substring(in).strip();

                running = "Running".equals(strStatus);
                sb.setLength(0);
                sb.append(name);
            }else{
                String name = line.strip();
                sb.append(name);
            }
        }

        if(sb.length() > 0){
            serviceInfoList.add(new ServiceInfo(running, sb.toString()));
        }

        return serviceInfoList;
    }

    public static Long getServicePid(String serviceName) throws Exception {
        List<String> lines = getCommandOutputLines(String.format("powershell -command \"tasklist /fi 'SERVICES eq %s' /fo csv | convertfrom-csv\"", serviceName));

        String strPid = null;
        for (String line : lines) {
            if(line.startsWith("PID")){
                strPid = line.substring(line.indexOf(":")+1).strip();
            }
        }

        if(StringUtil.isBlank(strPid)){
            return null;
        }else{
            return Long.valueOf(strPid);
        }
    }
}
