package com.xiyuan.spider.manager;

import com.xiyuan.cluster.msg.Messages;
import com.xiyuan.common.util.ClassUtil;
import com.xiyuan.common.util.FileUtil;
import com.xiyuan.config.AppInfo;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by xiyuan_fengyu on 2017/3/6.
 */
public class ShareFilesManager {

    private static Messages.Prt.ShareOnWorkerConnected shareOnWorkerConnected;

    public static Messages.Prt.ShareOnWorkerConnected getShareOnWorkerConnected() {
        return shareOnWorkerConnected;
    }

    public static final ArrayList<Messages.Prt.ShareFile> shareFiles = new ArrayList<>();

    public static void loadShareFiles() {
        shareFiles.clear();
        scanFile();
        shareOnWorkerConnected = Messages.Prt.ShareOnWorkerConnected.newBuilder().addAllShareFiles(shareFiles).build();
    }

    private static void scanFile() {
        File root = new File(AppInfo.getSrcPath());
        if (root.exists()) {
            scanFile(AppInfo.getSrcPath() + "/", root);
        }
    }

    private static void scanFile(String rootPath, File curFile) {
        if (curFile.isDirectory()) {
            File[] files = curFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    scanFile(rootPath, file);
                }
            }
        }
        else if (curFile.getName().endsWith(".js")) {
            String relativePath = curFile.getAbsolutePath().substring(rootPath.length()).replaceAll("\\\\", "/");
            shareFiles.add(
                    Messages.Prt.ShareFile.newBuilder()
                            .setRelativePath(relativePath)
                            .setContent(FileUtil.string(curFile))
                            .build()
            );
        }
    }

}
