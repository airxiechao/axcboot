package com.airxiechao.axcboot.util;

import java.net.*;
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

    public static String getIp(boolean v4){
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // filters out 127.0.0.1 and inactive interfaces
                if (iface.isLoopback() || !iface.isUp())
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if(v4){
                        if(addr instanceof Inet6Address){
                            continue;
                        }
                    }

                    String ip = addr.getHostAddress();
                    return ip;
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException("get ip error", e);
        }

        return null;
    }
}
