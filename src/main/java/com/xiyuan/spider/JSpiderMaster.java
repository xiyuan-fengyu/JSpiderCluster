package com.xiyuan.spider;

import com.xiyuan.common.loader.LoadFromClass;
import com.xiyuan.common.loader.LoadFromJar;
import com.xiyuan.common.util.ClassUtil;
import com.xiyuan.config.AppInfo;
import com.xiyuan.spider.manager.FileManager;
import com.xiyuan.spider.manager.ShareFilesManager;
import com.xiyuan.spider.manager.TaskManager;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by xiyuan_fengyu on 2017/3/3.
 */
public class JSpiderMaster {

    public static void reload() {
        //重新加载 jar 和 classes
        LoadFromJar.load(new File(AppInfo.getSrcPath()));

        //扫描 注解
        TaskManager.annotationScan(LoadFromClass.load(new File(AppInfo.getSrcPath())));

        //扫描 js 和 配置文件
        ShareFilesManager.loadShareFiles();

        //启动文件检测
        FileManager.newFileWatcher();
    }

}
