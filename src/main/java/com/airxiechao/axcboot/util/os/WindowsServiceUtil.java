package com.airxiechao.axcboot.util.os;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class WindowsServiceUtil {

    public static List<ServiceInfo> listService() throws Exception {
        Process process = Runtime.getRuntime().exec("powershell -command \"Get-Service | Select Status, Name | format-table -wrap\"");
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));

        List<ServiceInfo> serviceInfoList = new ArrayList<>();
        Boolean running = null;
        StringBuilder sb = new StringBuilder();

        String line;
        int in = 0;
        int no = 0;
        while ((line = br.readLine()) != null) {
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
}
