package com.xiyuan.spider.manager;

import com.xiyuan.cluster.controller.MasterController;
import com.xiyuan.cluster.msg.Messages;
import com.xiyuan.common.loader.LoadFromClass;
import com.xiyuan.common.loader.LoadFromJar;
import com.xiyuan.common.util.FileUtil;
import com.xiyuan.common.watcher.FileWatchers;
import com.xiyuan.config.AppInfo;

import java.io.File;

/**
 * Created by xiyuan_fengyu on 2017/3/7.
 */
public class FileManager {

    public static void newFileWatcher() {
        //监听 src下 js， class， jar 的改变
        FileWatchers.add(new FileWatchers.FileListener() {
            @Override
            public void onChange(File file) {
                onSrcChange(file);
            }
        }, AppInfo.getSrcPath(), new String[]{
            AppInfo.getConfigPath(), AppInfo.getCachePath(), AppInfo.getLogPath(), AppInfo.getOutPath(), AppInfo.getScreenshotPath(), AppInfo.getDownloadPath()
        });
    }

    public static void onSrcChange(File file) {
        if (file.isFile()) {
            String suffix = FileUtil.getSubffix(file.getName());

            switch (suffix) {
                case "jar": {
                    LoadFromJar.load(file);
                    break;
                }
                case "class": {
                    TaskManager.annotationScan(LoadFromClass.load(new File(AppInfo.getSrcPath())));
                    break;
                }
                case "js": {
                    ShareFilesManager.loadShareFiles();

                    String changeFile = file.getAbsolutePath();
                    Messages.Prt.ShareOnFileChanged.Builder builder = Messages.Prt.ShareOnFileChanged.newBuilder();
                    for (Messages.Prt.ShareFile shareFile : ShareFilesManager.shareFiles) {
                        if (changeFile.endsWith(shareFile.getRelativePath())) {
                            builder.addShareFiles(shareFile);
                        }
                    }
                    MasterController.copyShareFileToAllWorkers(builder.build());
                    break;
                }
            }

        }

    }

}
