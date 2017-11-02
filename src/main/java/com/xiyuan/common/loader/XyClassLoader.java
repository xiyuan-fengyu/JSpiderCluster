package com.xiyuan.common.loader;

import java.io.File;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class XyClassLoader extends URLClassLoader {

    public XyClassLoader() {
        super(new URL[]{});
    }

    private void addURL(File file) {
        try {
            super.addURL(file.toURI().toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public List<Class> load(String rootDir) {
        List<Class> classList = new ArrayList<>();
        if (rootDir != null) {
            File rootFile = new File(rootDir);
            rootDir = rootFile.getAbsolutePath().replace(File.separatorChar, '/');
            load(rootFile, rootDir, classList);
        }
        return classList;
    }

    private void load(File curDir, String rootDir, List<Class> classList) {
        addURL(curDir);

        File[] files = curDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    load(file, rootDir, classList);
                }
                else {
                    addURL(file);

                    String flieNameLower = file.getName().toLowerCase();
                    String abs = file.getAbsolutePath();
                    if (flieNameLower.endsWith(".class")) {
                        String className = abs.substring(rootDir.length() + 1, abs.length() - 6).replace(File.separatorChar, '.');
                        try {
                            classList.add(loadClass(className));
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    else if (flieNameLower.endsWith(".jar")) {
                        classList.addAll(loadFromJar(abs));
                    }
                }
            }
        }
    }

    private List<Class> loadFromJar(String jarPath) {
        List<Class> classList = new ArrayList<>();
        try {
            try (JarInputStream in = new JarInputStream(new FileInputStream(jarPath))) {
                JarEntry jarEntry;
                while ((jarEntry = in.getNextJarEntry()) != null) {
                    String name = jarEntry.getName();
                    if (name.endsWith(".class")) {
                        String classPath = name.replace('/', '.').substring(0, name.length() - 6);
                        classList.add(loadClass(classPath));
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return classList;
    }

}
