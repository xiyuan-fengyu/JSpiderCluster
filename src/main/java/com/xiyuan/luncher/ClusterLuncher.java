package com.xiyuan.luncher;

/**
 * Created by xiyuan_fengyu on 2017/3/3.
 */
public class ClusterLuncher {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("node type parameter required");
        }
        else {
            String nodeType = args[0];
            if ("master".equals(nodeType)) {
                MasterLuncher.startMaster();
            }
            else if ("worker".equals(nodeType)) {
                WorkerLuncher.startWorker();
            }
        }
    }

}