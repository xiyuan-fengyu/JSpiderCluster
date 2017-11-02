package com.xiyuan.common.loader;

import java.io.*;

/**
 * Created by xiyuan_fengyu on 2017/3/2.
 */
@Deprecated
class FileClassLoader extends ClassLoader {

    private String rootDir;

    FileClassLoader(String rootDir) {
        this.rootDir = rootDir;
    }

    Class findClass(String name, boolean forceReload) throws ClassNotFoundException {
        Class clazz = null;
        if (!forceReload) {
            try {
                clazz = loadClass(name);
            } catch (Exception ee) {
                //
            }
        }
        if (clazz != null) {
            return clazz;
        }
        else {
            byte[] classBytes = getClassBytes(name);
            if (classBytes == null) {
                throw new ClassNotFoundException();
            }
            else {
                clazz = defineClass(name, classBytes, 0, classBytes.length);
            }
        }
        return clazz;
    }

    private byte[] getClassBytes(String classname) {
        String path = rootDir + classname.replace(".", File.separator) + ".class";
        try (
                InputStream in = new FileInputStream(path);
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ) {
            byte[] arr = new byte[1024];
            int len;
            while ((len = in.read(arr)) != -1) {
                byteOut.write(arr, 0, len);
            }
            return byteOut.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

