package com.xiyuan.common.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by xiyuan_fengyu on 2017/2/16.
 */
public class HttpUtil {

    public static String get(String url) throws IOException {
        URL tempUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) tempUrl.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(false);
        connection.setUseCaches(false);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Charset", "UTF-8");
        connection.connect();

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
        String line;
        StringBuilder strBld = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            strBld.append(line).append("\n");
        }
        reader.close();
        return strBld.toString();
    }

}
