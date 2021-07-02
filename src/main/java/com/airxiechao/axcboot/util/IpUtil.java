package com.airxiechao.axcboot.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class IpUtil {

    public static List<String> getIps(){

        List<String> ips = new ArrayList<>();
        try{
            final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while(interfaces.hasMoreElements()) {
                final NetworkInterface cur = interfaces.nextElement();

                if (cur.isLoopback()) {
                    continue;
                }

                List<InterfaceAddress> addresses = cur.getInterfaceAddresses();
                if (addresses.size() == 0) {
                    continue;
                }

                for (InterfaceAddress address : addresses) {
                    final InetAddress inet_address = address.getAddress();

                    if (!(inet_address instanceof Inet4Address)) {
                        continue;
                    }

                    String ip = inet_address.getHostAddress();
                    ips.add(ip);
                }
            }
        }catch (Exception e){
            throw new RuntimeException("get ips error", e);
        }

        return ips;
    }
}
