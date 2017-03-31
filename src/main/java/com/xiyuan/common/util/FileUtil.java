package com.xiyuan.common.util;

import com.xiyuan.cluster.msg.Messages;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * Created by xiyuan_fengyu on 2017/2/20.
 */
public class FileUtil {

    public static String getAbsPathAndCopyIfInJar(String relativePath) {
        if (relativePath == null || "".equals(relativePath)) {
            return "";
        }

        String tempPath;
        if (ClassUtil.isExcuteInJar()) {
            try {
                File tempJs = File.createTempFile("temp", '.' + getSubffix(relativePath));
                tempJs.deleteOnExit();
                Files.copy(FileUtil.class.getClassLoader().getResourceAsStream(relativePath), tempJs.toPath(), StandardCopyOption.REPLACE_EXISTING);
                tempPath = tempJs.getPath();
            } catch (IOException e) {
                e.printStackTrace();
                tempPath = "";
            }
        }
        else {
            tempPath = ClassUtil.classRoot + "/" + relativePath;
        }

        return tempPath;
    }

    public static byte[] bytes(File file) {
        byte[] result;
        try (InputStream in = new FileInputStream(file)) {
            result = new byte[in.available()];
            in.read(result);
        }
        catch (Exception e) {
            e.printStackTrace();
            result = new byte[0];
        }
        return result;
    }

    public static String string(File file) {
        return string(file, StandardCharsets.UTF_8);
    }

    public static String string(File file, Charset charset) {
        StringBuilder buffer = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset))) {
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append('\n');
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return buffer.toString();
    }

    public static boolean write(String path, String content) {
        return write(path, content, null);
    }

    public static boolean write(String path, String content, Charset charset) {
        if (content == null) {
            return false;
        }
        return write(path, content.getBytes(charset == null ? StandardCharsets.UTF_8 : charset), StandardOpenOption.CREATE);
    }

    public static boolean append(String path, String content) {
        return append(path, content, null);
    }

    public static boolean append(String path, String content, Charset charset) {
        if (content == null) {
            return false;
        }
        return append(path, content.getBytes(charset == null ? StandardCharsets.UTF_8 : charset));
    }

    public static boolean append(String path, byte[] bytes) {
        return write(path, bytes, StandardOpenOption.APPEND);
    }

    public static boolean write(String path, byte[] bytes) {
        return write(path, bytes, StandardOpenOption.CREATE);
    }

    public static boolean write(String path, byte[] bytes, StandardOpenOption openOption) {
        if (bytes == null) {
            return false;
        }

        File file = new File(path);
        File dir = file.getParentFile();
        if (dir != null && (dir.exists() || dir.mkdirs())) {
            try {
                if (!file.exists() && StandardOpenOption.APPEND == openOption) {
                    file.createNewFile();
                }

                Files.write(file.toPath(), bytes, openOption);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
        else return false;
    }

    public static String getSubffix(String path) {
        if (path == null || "".equals(path)) {
            return "";
        }

        int len = path.length();
        for (int i = len - 1; i > -1; i--) {
            char c = path.charAt(i);
            if (c == '.') {
                return path.substring(i + 1);
            }
            else if (c == File.separatorChar) {
                return "";
            }
        }
        return "";
    }

}
