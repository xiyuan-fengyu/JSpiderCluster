package com.xiyuan.common.util;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by xiyuan_fengyu on 2017/3/3.
 */
public class IpPortUtil {

    public static Set<String> localIps() {
        HashSet<String> ips = new HashSet<>();
        try {
            Enumeration<NetworkInterface> items = NetworkInterface.getNetworkInterfaces();
            while (items.hasMoreElements()) {
                Enumeration<InetAddress> addresses = items.nextElement().getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address.getHostAddress().indexOf(':') == -1) {
                        ips.add(address.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return ips;
    }

    private static void bindPort(String host, int port) throws IOException {
        Socket s = new Socket();
        if (host != null) {
            s.bind(new InetSocketAddress(host, port));
        }
        else {
            s.bind(new InetSocketAddress(port));
        }
        s.close();
    }

    private static final String[] localhosts = {
            "127.0.0.1"
    };

    public static boolean isPortAvailable(int port) {
        boolean available = true;

        for (String ip : localhosts) {
            try {
                bindPort(ip, port);
            }
            catch (BindException e) {
                available = false;
                System.out.println(ip + ":" + port + " is aready in use");
                break;
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return available;
    }

    public static void main(String[] args) throws UnknownHostException {
        for (String s : localIps()) {
            System.out.println(s);
        }
        System.out.println(isPortAvailable(8000));
    }

}
