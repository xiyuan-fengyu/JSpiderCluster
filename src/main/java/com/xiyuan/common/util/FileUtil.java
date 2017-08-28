package com.xiyuan.common.util;

import com.xiyuan.cluster.msg.Messages;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;

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

    public static boolean write(String path, byte[] bytes) {
        return write(path, bytes, false);
    }

    public static boolean write(String path, String content) {
        return write(path, content, null);
    }

    public static boolean write(String path, String content, Charset charset) {
        if (content == null) {
            return false;
        }
        return write(path, content.getBytes(charset != null ? charset : StandardCharsets.UTF_8), false);
    }


    public static boolean append(String path, byte[] bytes) {
        return write(path, bytes, true);
    }

    public static boolean append(String path, String content) {
        return append(path, content, null);
    }

    public static boolean append(String path, String content, Charset charset) {
        if (content == null) {
            return false;
        }
        return write(path, content.getBytes(charset != null ? charset : StandardCharsets.UTF_8), true);
    }

    private static boolean write(String path, byte[] bytes, boolean isAppend) {
        try (FileOutputStream out = new FileOutputStream(path, isAppend)) {
            out.write(bytes);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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

    public static ArrayList<File> listFile(File dir) {
        ArrayList<File> files = new ArrayList<>();
        if (dir != null && dir.exists()) {
            if (dir.isFile()) {
                files.add(dir);
            }
            else {
                File[] fs = dir.listFiles();
                if (fs != null) {
                    files.addAll(Arrays.asList(fs));
                }
            }
        }
        return files;
    }

}
