package com.xiyuan.common.loader;

import com.xiyuan.common.tuple.Tuple2;
import com.xiyuan.common.util.FileUtil;
import com.xiyuan.common.util.Md5Util;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by xiyuan_fengyu on 2017/3/2.
 */
@Deprecated
public class LoadFromClass {

    public static List<Class> load(File classRoot) {
        ArrayList<Class> classes = new ArrayList<>();

        if (classRoot.exists() && classRoot.isDirectory()) {
            String classRootPath = classRoot.getAbsolutePath();
            if (classRootPath.charAt(classRootPath.length() - 1) != File.separatorChar) {
                classRootPath += File.separatorChar;
            }
            FileClassLoader loader = new FileClassLoader(classRootPath);

            ArrayList<Tuple2<String, Boolean>> classNames = new ArrayList<>();
            load(classRootPath, classRoot, classNames);

            for (Tuple2<String, Boolean> className : classNames) {
                try {
                    classes.add(loader.findClass(className._1, className._2));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return classes;
    }

    private static final HashMap<String, String> fileMd5s = new HashMap<>();

    private static void load(String classRootPath, File curFile, ArrayList<Tuple2<String, Boolean>> classNames) {
        if (curFile.isDirectory()) {
            File[] files = curFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        load(classRootPath, file, classNames);
                    }
                    else if (file.getName().endsWith(".class")) {
                        String className = file.getAbsolutePath();
                        className = className.substring(classRootPath.length(), className.length() - 6).replace(File.separatorChar, '.');
                        boolean forceReload = false;
                        String oldMd5 = fileMd5s.get(className);
                        String newMd5 = Md5Util.get(FileUtil.string(file), StandardCharsets.UTF_8);
                        if (oldMd5 != null && !newMd5.equals(oldMd5)) {
                            forceReload = true;
                        }
                        fileMd5s.put(className, newMd5);
                        classNames.add(new Tuple2<>(className, forceReload));
                    }
                }
            }
        }
    }

}
