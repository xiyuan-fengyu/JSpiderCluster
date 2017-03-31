package com.xiyuan.common.util;

import java.io.File;
import java.io.FileFilter;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by xiyuan_fengyu on 2017/2/16.
 */
public class ClassUtil {

    private static final String tagFile = "file:";

    public static boolean isExcuteInJar() {
        return ClassUtil.class.getResource(ClassUtil.class.getSimpleName() + ".class").getPath().startsWith(tagFile);
    }

    public static final String classRoot = getClassRoot(ClassUtil.class);

    public static String getClassRoot(Class<?> clazz) {
        if (clazz == null) {
            clazz = ClassUtil.class;
        }

        String classPath = "/" + (clazz.getPackage() == null ? "" : clazz.getPackage().getName().replaceAll("\\.", "/") + "/") + clazz.getSimpleName() + ".class";

        String tempPath;
        String path = clazz.getResource(clazz.getSimpleName() + ".class").getPath();
        if (path.startsWith(tagFile)) {
            String jarPath = path.substring(tagFile.length(), path.indexOf(classPath));
            tempPath = jarPath.substring(0, jarPath.lastIndexOf("/"));
        }
        else {
            tempPath = path.substring(0, path.indexOf(classPath));
        }
        return new File(tempPath).getPath().replaceAll("\\\\", "/");
    }

    public static HashSet<Class> getClasses(String pack, boolean recursive) {
        HashSet<Class> classes = new HashSet<>();
        String packageName = pack;
        String packageDirName = packageName.replace('.', '/');
        Enumeration<URL> dirs = null;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            while (dirs.hasMoreElements()) {
                URL url = dirs.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    findAndAddClassesInPackageByFile(packageName, filePath, recursive, classes);
                }
                else if ("jar".equals(protocol)) {
                    JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (name.charAt(0) == '/') {
                            name = name.substring(1);
                        }
                        if (name.startsWith(packageDirName)) {
                            int idx = name.lastIndexOf('/');
                            if (idx != -1) {
                                packageName = name.substring(0, idx).replace('/', '.');
                            }

                            if (idx != -1 || recursive) {
                                if (name.endsWith(".class") && !entry.isDirectory()) {
                                    String className = name.substring(packageName.length() + 1, name.length() - 6);
                                    try {
                                        String wholeClassName = packageName + '.' + className;
                                        if (wholeClassName.charAt(0) == '.') {
                                            wholeClassName = wholeClassName.substring(1);
                                        }
                                        classes.add(Class.forName(wholeClassName));
                                    }
                                    catch (Exception ee) {
                                        ee.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return classes;
    }

    private static void findAndAddClassesInPackageByFile(final String packageName, String packagePath, final boolean recursive, HashSet<Class> classes) {
        File dir = new File(packagePath);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        File[] dirFiels = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return (recursive && file.isDirectory()) || file.getName().endsWith(".class");
            }
        });

        if (dirFiels != null) {
            for (File file: dirFiels) {
                if (file.isDirectory()) {
                    findAndAddClassesInPackageByFile(packageName + '.' + file.getName(), file.getAbsolutePath(), recursive, classes);
                }
                else {
                    String fileName = file.getName();
                    String className = fileName.substring(0, fileName.length() - 6);
                    try {
                        String wholeClassName = packageName + '.' + className;
                        if (wholeClassName.charAt(0) == '.') {
                            wholeClassName = wholeClassName.substring(1);
                        }
                        classes.add(Thread.currentThread().getContextClassLoader().loadClass(wholeClassName));
                    }
                    catch (Exception ee) {
                        ee.printStackTrace();
                    }
                }
            }
        }
    }

    public static Class getCallerClass() {
        StackTraceElement[] traces = new Throwable().getStackTrace();
        for (int i = 2; i < traces.length; i++) {
            StackTraceElement trace = traces[i];
            if (!trace.getMethodName().matches("access\\$[0-9]+")) {
                try {
                    return Class.forName(trace.getClassName());
                }
                catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

}
