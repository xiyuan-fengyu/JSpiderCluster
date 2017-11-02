package com.xiyuan.common.loader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xiyuan_fengyu on 2017/3/2.
 */
@Deprecated
public class LoadFromJar {

    public static void load(File dir) {
        if (dir.exists()) {
            ArrayList<URL> urls = new ArrayList<>();
            findAllJarUrl(dir, urls);
            addUrlsToClassPath(urls);
        }
    }

    public static void addUrlsToClassPath(List<URL> urls) {
        if (urls != null && urls.size() > 0) {
//            try {
//                Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
//                method.setAccessible(true);
//                URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
//                for (URL url : urls) {
//                    try {
//                        method.invoke(classLoader, url);
//                    } catch (IllegalAccessException | InvocationTargetException e) {
//                        e.printStackTrace();
//                    }
//                }
//            } catch (NoSuchMethodException e) {
//                e.printStackTrace();
//            }

//            URL[] urlArr = new URL[urls.size()];
//            urls.toArray(urlArr);
//            for (URL url : urlArr) {
//                System.out.println(url);
//            }
//            URLClassLoader classLoader = new URLClassLoader(urlArr, LoadFromJar.class.getClassLoader());
//            try {
//                classLoader.loadClass("com.github.kevinsawicki.http.HttpRequest");
//                classLoader.close();
//            } catch (ClassNotFoundException | IOException e) {
//                e.printStackTrace();
//            }


        }
    }

    private static void findAllJarUrl(File file, List<URL> jarUrls) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File temp : files) {
                    findAllJarUrl(temp, jarUrls);
                }
            }
        }
        else {
            addUrl(file, jarUrls);
        }
    }

    private static void addUrl(File file, List<URL> urls) {
        if (file.getName().endsWith(".jar")) {
            try {
                urls.add(file.toURI().toURL());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        load(new File("D:\\SoftwareForCode\\MyEclipseProject\\NettyLearning\\target\\libs\\netty-all-5.0.0.Alpha2.jar"));
    }

}
