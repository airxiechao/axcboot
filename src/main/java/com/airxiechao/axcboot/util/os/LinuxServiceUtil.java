package com.airxiechao.axcboot.util.os;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LinuxServiceUtil {

    public static List<ServiceInfo> listService() throws Exception {
        Process process = Runtime.getRuntime().exec("systemctl list-units --no-pager --no-legend --all");
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));

        List<ServiceInfo> serviceInfoList = new ArrayList<>();

        String line;
        int no = 0;
        while ((line = br.readLine()) != null) {
            no++;

            String[] tokens = line.split("\\s+");
            String name = tokens[0].strip();
            boolean running = "active".equals(tokens[2].strip());
            serviceInfoList.add(new ServiceInfo(running, name));
        }

        return serviceInfoList;
    }
}
