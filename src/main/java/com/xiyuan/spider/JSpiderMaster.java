package com.xiyuan.spider;

import com.xiyuan.common.loader.XyClassLoader;
import com.xiyuan.config.AppInfo;
import com.xiyuan.spider.manager.FileManager;
import com.xiyuan.spider.manager.ShareFilesManager;
import com.xiyuan.spider.manager.TaskManager;

/**
 * Created by xiyuan_fengyu on 2017/3/3.
 */
public class JSpiderMaster {

    public static void reload() {
        //重新加载 jar 和 classes 并扫描注解
        TaskManager.annotationScan(new XyClassLoader().load(AppInfo.getSrcPath()));

        //扫描 js 和 配置文件
        ShareFilesManager.loadShareFiles();

        //启动文件检测
        FileManager.newFileWatcher();
    }

}
