package com.xiyuan.luncher;

import com.xiyuan.cluster.node.Master;
import com.xiyuan.cluster.node.Worker;
import com.xiyuan.common.log.LogManager;
import com.xiyuan.common.util.ClassUtil;
import com.xiyuan.common.util.IpPortUtil;
import com.xiyuan.config.AppInfo;
import com.xiyuan.config.ClusterCfg;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;

/**
 * Created by xiyuan_fengyu on 2017/3/3.
 */
public class WorkerLuncher {

    private static Logger logger;

    public static void startWorker() {
        AppInfo.setPath(ClassUtil.getCallerClass());
        logger = LogManager.logger(Master.class);

        new Thread() {
            @Override
            public void run() {
                Set<String> localIps = IpPortUtil.localIps();
                int workerNum = 0;
                for (Map.Entry<String, Set<ClusterCfg.WorkerCfg>> keyVal : ClusterCfg.cluster_workers.entrySet()) {
                    String host = keyVal.getKey();
                    if (localIps.contains(host)) {
                        for (ClusterCfg.WorkerCfg workerCfg : keyVal.getValue()) {
                            int phantomPortValid = 0;
                            for (int phantomPort : workerCfg.phantom_ports) {
                                if (IpPortUtil.isPortAvailable(phantomPort)) {
                                    phantomPortValid += 1;
                                }
                            }

                            if (phantomPortValid > 0) {
                                workerNum += 1;

                                new Worker(workerCfg);
                            }
                        }
                    }
                }

                if (workerNum == 0) {
                    logger.warn("no worker to create");
                }
            }
        }.start();
    }

}
