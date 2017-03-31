package com.xiyuan.cluster.controller;

import com.xiyuan.cluster.msg.Messages;
import com.xiyuan.common.util.ClassUtil;
import com.xiyuan.config.AppInfo;
import com.xiyuan.config.ClusterCfg;
import com.xiyuan.spider.JSpiderWorker;
import io.netty.channel.ChannelHandlerContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Created by xiyuan_fengyu on 2017/3/3.
 */
public class WorkerController {

    private ClusterCfg.WorkerCfg workerCfg;

    public WorkerController(ClusterCfg.WorkerCfg workerCfg) {
        this.workerCfg = workerCfg;
    }

    public void channelActive(ChannelHandlerContext ctx) {
        JSpiderWorker.startFileWatcher(ctx);
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Messages.Prt.ShareOnWorkerConnected) {
            Messages.Prt.ShareOnWorkerConnected obj = (Messages.Prt.ShareOnWorkerConnected) msg;
            for (Messages.Prt.ShareFile shareFile : obj.getShareFilesList()) {
                saveShareFile(shareFile);
            }
            JSpiderWorker.startPhantoms(ctx, workerCfg);
        }
        else if (msg instanceof Messages.Prt.ShareOnFileChanged) {
            Messages.Prt.ShareOnFileChanged obj = (Messages.Prt.ShareOnFileChanged) msg;
            boolean shouldRestart = false;
            for (Messages.Prt.ShareFile shareFile : obj.getShareFilesList()) {
                saveShareFile(shareFile);
                if (!shouldRestart && shareFile.getRelativePath().endsWith("phantom.json")) {
                    shouldRestart = true;
                }
            }
            if (shouldRestart) {
                JSpiderWorker.restartAllPhantom();
            }
        }
        else if (msg instanceof Messages.Prt.RestartPhantomServer) {
            Messages.Prt.RestartPhantomServer obj = (Messages.Prt.RestartPhantomServer) msg;
            JSpiderWorker.restartPhantom(obj.getPort());
        }
        else if (msg instanceof Messages.Prt.StopPhantomServer) {
            Messages.Prt.StopPhantomServer obj = (Messages.Prt.StopPhantomServer) msg;
            JSpiderWorker.stopPhantom(obj.getPort());
        }
        else if (msg instanceof Messages.Prt.NewPhantomServer) {
            Messages.Prt.NewPhantomServer obj = (Messages.Prt.NewPhantomServer) msg;
            JSpiderWorker.startPhantom(ctx, obj.getPort());
        }
    }

    private void saveShareFile(Messages.Prt.ShareFile shareFile) {
        File jsFile = new File(AppInfo.getSrcPath() + "/" + shareFile.getRelativePath());
        File dir = jsFile.getParentFile();
        if (dir.exists() || dir.mkdirs()) {
            try {
                Files.write(jsFile.toPath(), shareFile.getContent().getBytes("utf-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void destory() {
        JSpiderWorker.stopPhantoms(workerCfg);
        workerCfg = null;
    }

}
