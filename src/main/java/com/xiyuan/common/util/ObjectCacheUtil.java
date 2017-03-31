package com.xiyuan.common.util;

import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;

import java.io.*;

/**
 * Created by xiyuan_fengyu on 2017/3/15.
 */
public class ObjectCacheUtil {

    public static void save(Object obj, String path) {
        if (obj != null) {
            File cache = new File(path);
            File dir = cache.getParentFile();
            if (dir.exists() || dir.mkdirs()) {
                try (ObjectOutputStream out = new ObjectOutputStream(new LZ4BlockOutputStream(new FileOutputStream(cache)))) {
                    out.writeObject(obj);
                    out.flush();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static Object load(String path) {
        if (path != null) {
            File cache = new File(path);
            if (cache.exists()) {
                try (ObjectInputStream in = new ObjectInputStream(new LZ4BlockInputStream(new FileInputStream(cache)))) {
                    return in.readObject();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

}
