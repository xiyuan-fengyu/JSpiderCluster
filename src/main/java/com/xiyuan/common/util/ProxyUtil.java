package com.xiyuan.common.util;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by xiyuan_fengyu on 2017/5/24.
 */
public class ProxyUtil {

    public static boolean valid(String ip, int port) {
        return valid(ip, port, "http://www.baidu.com", 5000);
    }

    public static boolean valid(String ip, int port, String checkUrl, int timeout) {
        try {
            URL url = new URL(checkUrl);
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ip, port));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
            connection.setConnectTimeout(timeout);
//            connection.setRequestProperty("");
            connection.connect();
            return connection.getResponseCode() == 200 || "OK".equals(connection.getResponseMessage());
        } catch (IOException e) {
            return false;
        }
    }

    public static void main(String[] args) {
        try {
            List<String> proxys = Files.readAllLines(Paths.get("D:\\SoftwareForCode\\MyEclipseProject\\JSpiderCluster\\src\\test\\resources\\data\\proxys.data"), StandardCharsets.UTF_8);
            for (String proxy : proxys) {
                String[] split = proxy.split(":");
                String ip = split[0];
                int port = Integer.parseInt(split[1]);
                if (valid(ip, port)) {
                    System.out.println(proxy);
                }
                else {
                    System.err.println("BAD\t" + proxy);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
