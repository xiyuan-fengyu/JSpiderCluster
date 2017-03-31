package com.xiyuan.luncher;

import com.xiyuan.cluster.node.Master;
import com.xiyuan.common.log.LogManager;
import com.xiyuan.common.util.ClassUtil;
import com.xiyuan.common.util.IpPortUtil;
import com.xiyuan.config.AppInfo;
import com.xiyuan.config.ClusterCfg;
import com.xiyuan.spider.JSpiderMaster;
import com.xiyuan.webui.WebUI;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * Created by xiyuan_fengyu on 2017/3/3.
 */
public class MasterLuncher {

    private static Master master;

    private static final CountDownLatch latch = new CountDownLatch(1);

    private static Logger logger;

    public static void startMaster() {
        AppInfo.setPath(ClassUtil.getCallerClass());
        logger = LogManager.logger(Master.class);

        new Thread() {
            @Override
            public void run() {
                Set<String> localIps = IpPortUtil.localIps();
                if (localIps.contains(ClusterCfg.cluster_master_host)) {
                    if (IpPortUtil.isPortAvailable(ClusterCfg.cluster_master_netty_port)) {
                        JSpiderMaster.reload();

                        //启动 master WebUI
                        WebUI.startWebUI();

                        //启动 master
                        master = new Master(ClusterCfg.cluster_master_netty_port);

                        try {
                            //唤醒 latch.wait(); 中的等待
                            synchronized (latch) {
                                latch.notifyAll();
                            }

                            //阻塞线程，阻止退出
                            latch.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        logger.error("The " + ClusterCfg.cluster_master_netty_port + " port is aready in use!");
                    }
                }
                else {
                    logger.error("The master host was not right!");
                }
            }
        }.start();

        synchronized (latch) {
            try {
                //等待 master 启动完成
                latch.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void shutdown() {
        if (master != null) {
            master.shutdown();
            master = null;
        }
    }

    public static void exit() {
        latch.countDown();
    }

}
