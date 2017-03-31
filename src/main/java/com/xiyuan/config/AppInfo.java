package com.xiyuan.config;

import com.google.gson.JsonObject;
import com.xiyuan.cluster.controller.MasterController;
import com.xiyuan.common.loader.LoadFromJar;
import com.xiyuan.common.util.ClassUtil;
import com.xiyuan.luncher.ClusterLuncher;
import com.xiyuan.luncher.MasterLuncher;
import com.xiyuan.spider.manager.TaskManager;
import sun.reflect.Reflection;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by xiyuan_fengyu on 2017/3/7.
 */
public class AppInfo {

    private static boolean isMaster;

    private static String jspiderHome;

    private static String configPath;

    private static String logPath;

    private static String cachePath;

    private static String screenshotPath;

    private static String downloadPath;

    private static String srcPath;

    private static String outPath;

    public static boolean isMaster() {
        return isMaster;
    }

    public static String getJspiderHome() {
        return jspiderHome;
    }

    public static String getConfigPath() {
        return configPath;
    }

    public static String getLogPath() {
        return logPath;
    }

    public static String getCachePath() {
        return cachePath;
    }

    public static String getScreenshotPath() {
        return screenshotPath;
    }

    public static String getDownloadPath() {
        return downloadPath;
    }

    public static String getSrcPath() {
        return srcPath;
    }

    public static String getOutPath() {
        return outPath;
    }

    public static void setPath(Class<?> startFromClass) {
        isMaster = Reflection.getCallerClass() == MasterLuncher.class;

        String mianClassRoot = ClassUtil.getClassRoot(startFromClass);
        File mainClassRootDir = new File(mianClassRoot);

        if (startFromClass == ClusterLuncher.class) {
            jspiderHome = mainClassRootDir.getParent();
            srcPath = jspiderHome + "/src";
        }
        else {
            jspiderHome = mianClassRoot;
            srcPath = jspiderHome;
        }
        configPath = jspiderHome + "/config";
        logPath = jspiderHome + "/logs";
        cachePath = jspiderHome + "/cache";
        screenshotPath = jspiderHome + "/screenshot";
        downloadPath = jspiderHome + "/download";
        outPath = jspiderHome + "/out";

        //将scrPath提娜佳到classPath中
        try {
            LoadFromJar.addUrlsToClassPath(Arrays.asList(new File(srcPath).toURI().toURL()));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        System.out.println();
    }

    private static AtomicInteger successTaskNum = new AtomicInteger(0);

    private static AtomicInteger failTaskNum = new AtomicInteger(0);

    private static AtomicInteger runningTaskNum = new AtomicInteger(0);

    public static void deltaSuccessTaskNum(int delta) {
        successTaskNum.addAndGet(delta);
    }

    public static void deltaFailTaskNum(int delta) {
        failTaskNum.addAndGet(delta);
    }

    public static void deltaRunningTaskNum(int delta) {
        runningTaskNum.addAndGet(delta);
    }

    public static JsonObject clusterInfo() {
        JsonObject info = new JsonObject();
        info.addProperty("master", ClusterCfg.cluster_master_host + ":" + ClusterCfg.cluster_master_netty_port);
        info.addProperty("successTaskNum", successTaskNum);
        info.addProperty("runningTaskNum", runningTaskNum);
        info.addProperty("failTaskNum", failTaskNum);
        info.add("onStartTasks", TaskManager.getOnStartTasks());
        info.add("onTimeTasks", TaskManager.getOnTimeTasks());
        info.add("onMessageTasks", TaskManager.getOnMessageTasks());
        info.add("workers", MasterController.getWorkers());
        return info;
    }

}
