package com.xiyuan.common.watcher;

import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;

/**
 * Created by xiyuan_fengyu on 2017/3/27.
 */
public class FileWatchers {

    /**
     * @param listener
     * @param path 绝对地址
     * @param ignoreChildren 要忽略的文件夹或者文件，绝对路径
     */
    public static void add(final FileListener listener, final String path, final String[] ignoreChildren) {
        if (listener == null || path == null) {
            return;
        }

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    File file = new File(path);
                    if (!file.exists() && !file.mkdirs()) {
                        return;
                    }

                    final HashSet<String> ignores = new HashSet<>();
                    if (ignoreChildren != null) {
                        for (String child : ignoreChildren) {
                            ignores.add(child);
                        }
                    }

                    FileAlterationObserver observer = new FileAlterationObserver(file, new FileFilter() {
                        @Override
                        public boolean accept(File pathname) {
                            return !ignores.contains(pathname.getAbsolutePath());
                        }
                    });

                    observer.addListener(new FileAlterationListenerAdaptor() {
                        @Override
                        public void onDirectoryCreate(File directory) {
                            listener.onChange(directory);
                        }

                        @Override
                        public void onDirectoryChange(File directory) {
                            listener.onChange(directory);
                        }

                        @Override
                        public void onDirectoryDelete(File directory) {
                            listener.onChange(directory);
                        }

                        @Override
                        public void onFileCreate(File file) {
                            listener.onChange(file);
                        }

                        @Override
                        public void onFileChange(File file) {
                            listener.onChange(file);
                        }

                        @Override
                        public void onFileDelete(File file) {
                            listener.onChange(file);
                        }
                    });

                    FileAlterationMonitor monitor = new FileAlterationMonitor(3000, observer);
                    monitor.start();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
    }

    public interface FileListener {

        void onChange(File file);

    }

}
