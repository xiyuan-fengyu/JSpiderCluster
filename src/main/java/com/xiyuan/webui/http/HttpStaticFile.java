package com.xiyuan.webui.http;

import com.xiyuan.config.AppInfo;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Created by xiyuan_fengyu on 2017/3/9.
 */
public class HttpStaticFile {

    public static byte[] get(String path) {
        String tempPath = path.substring(1);
        byte[] bytes = readFromJar("web/" + tempPath);
        if (bytes == null) {
            bytes = readFromJar(tempPath);
            if (bytes == null) {
                bytes = readFromWorkplace(tempPath);
            }
        }
        return bytes;
    }

    private static byte[] readFromJar(String path) {
        try (InputStream in = HttpStaticFile.class.getClassLoader().getResourceAsStream(path)) {
            if (in != null) {
                byte[] bytes = new byte[in.available()];
                in.read(bytes);
                return bytes;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[] readFromWorkplace(String path) {
        try (InputStream in = new FileInputStream(new File(AppInfo.getSrcPath() + "/" + path))) {
            byte[] bytes = new byte[in.available()];
            in.read(bytes);
            return bytes;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
