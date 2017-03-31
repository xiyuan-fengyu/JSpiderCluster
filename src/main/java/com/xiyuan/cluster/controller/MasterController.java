package com.xiyuan.cluster.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xiyuan.cluster.msg.Messages;
import com.xiyuan.common.util.Base64Util;
import com.xiyuan.common.util.FileUtil;
import com.xiyuan.config.AppInfo;
import com.xiyuan.spider.manager.ShareFilesManager;
import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by xiyuan_fengyu on 2017/3/3.
 */
public class MasterController {

    private static final TreeMap<ChannelHandlerContext, WorkerInfo> workers = new TreeMap<>(new Comparator<ChannelHandlerContext>() {
        @Override
        public int compare(ChannelHandlerContext o1, ChannelHandlerContext o2) {
            return (o1 == null ? "" : ((InetSocketAddress) o1.channel().remoteAddress()).getHostString()).compareTo(o2 == null ? "" : ((InetSocketAddress) o2.channel().remoteAddress()).getHostString());
        }
    });

    private static final ArrayList<WorkerInfo.PhantomInfo> phantomServers = new ArrayList<>();

    public static JsonArray getWorkers() {
        JsonArray arr = new JsonArray();
        for (WorkerInfo workerInfo : workers.values()) {
            JsonObject worker = new JsonObject();
            worker.addProperty("address", workerInfo.host + ":" + workerInfo.port);
            JsonArray phantoms = new JsonArray();
            for (WorkerInfo.PhantomInfo info : workerInfo.phantoms.values()) {
                JsonObject phantom = new JsonObject();
                phantom.addProperty("port", info.port);
                phantom.addProperty("running", info.curTaskNum);
                phantom.addProperty("refused", info.refusedNum);
                phantoms.add(phantom);
            }
            worker.add("phantoms", phantoms);
            arr.add(worker);
        }
        return arr;
    }

    public static void channelActive(ChannelHandlerContext ctx) {
        workers.put(ctx, new WorkerInfo(ctx));
        copyShareFileToWorker(ctx);
    }

    public static void channelUnregistered(ChannelHandlerContext ctx) {
        synchronized (phantomServers) {
            workers.remove(ctx);
            refreshPhantomServer();
        }
    }

    public static void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Messages.Prt.PhantomStart) {
            synchronized (phantomServers) {
                workers.get(ctx).phantomStart(((Messages.Prt.PhantomStart) msg).getPort());
                refreshPhantomServer();
            }
        }
        else if (msg instanceof Messages.Prt.PhantomEnd) {
            synchronized (phantomServers) {
                workers.get(ctx).phantomStop(((Messages.Prt.PhantomEnd) msg).getPort());
                refreshPhantomServer();
            }
        }
        else if (msg instanceof Messages.Prt.ScreenshotFile) {
            Messages.Prt.ScreenshotFile screenshotFile = (Messages.Prt.ScreenshotFile) msg;
            for (Messages.Prt.ShareFile shareFile : screenshotFile.getShareFilesList()) {
                FileUtil.write(AppInfo.getScreenshotPath() + "/" + shareFile.getRelativePath(), shareFile.getContentBytes().toByteArray());
            }
        }
        else if (msg instanceof Messages.Prt.DownloadFile) {
            Messages.Prt.DownloadFile downloadFile = (Messages.Prt.DownloadFile) msg;
            for (Messages.Prt.ShareFile shareFile : downloadFile.getShareFilesList()) {
                FileUtil.write(AppInfo.getDownloadPath() + "/" + shareFile.getRelativePath(), shareFile.getContentBytes().toByteArray());
            }
        }
    }

    private static void copyShareFileToWorker(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(ShareFilesManager.getShareOnWorkerConnected());
    }

    public static void copyShareFileToAllWorkers(Messages.Prt.ShareOnFileChanged msg) {
        for (ChannelHandlerContext ctx : workers.keySet()) {
            ctx.writeAndFlush(msg);
        }
    }

    public static ArrayList<WorkerInfo.PhantomInfo> getPhantomServers() {
        synchronized (phantomServers) {
            long now = System.currentTimeMillis();
            for (int i = phantomServers.size() - 1; i >= 0; i--) {
                WorkerInfo.PhantomInfo server = phantomServers.get(i);
                if (server.firstRefusedTime != 0 && now - server.firstRefusedTime > WorkerInfo.PhantomInfo.timeToRestartAfterRefused) {
                    server.firstRefusedTime = 0;
                    restartPhantomServer(server.host, server.port);
                }
            }
            return  phantomServers;
        }
    }

    private static void refreshPhantomServer() {
        phantomServers.clear();
        for (WorkerInfo workerInfo : workers.values()) {
            for (WorkerInfo.PhantomInfo phantomInfo : workerInfo.phantoms.values()) {
                phantomServers.add(phantomInfo);
            }
        }
    }

    public static void restartPhantomServer(String host, int port) {
        for (Map.Entry<ChannelHandlerContext, WorkerInfo> entry : workers.entrySet()) {
            WorkerInfo workerInfo = entry.getValue();
            if (workerInfo.host.equals(host) && workerInfo.phantoms.containsKey(port)) {
                entry.getKey().writeAndFlush(Messages.Prt.RestartPhantomServer.newBuilder().setPort(port).build());
                break;
            }
        }
    }

    public static void stopPhantomServer(String host, int port) {
        for (Map.Entry<ChannelHandlerContext, WorkerInfo> entry : workers.entrySet()) {
            WorkerInfo workerInfo = entry.getValue();
            if (workerInfo.host.equals(host) && workerInfo.phantoms.containsKey(port)) {
                entry.getKey().writeAndFlush(Messages.Prt.StopPhantomServer.newBuilder().setPort(port).build());
                workerInfo.phantoms.remove(port);
                refreshPhantomServer();
                break;
            }
        }
    }

    public static JsonObject newPhantomServer(String host, int port) {
        JsonObject result = new JsonObject();
        if (port <= 0) {
            result.addProperty("success", false);
            result.addProperty("message", "The port must be a positive integer !");
            return result;
        }

        ChannelHandlerContext ctx = null;
        boolean isExisted = false;
        for (Map.Entry<ChannelHandlerContext, WorkerInfo> entry : workers.entrySet()) {
            WorkerInfo workerInfo = entry.getValue();
            if (workerInfo.host.equals(host)) {
                if (workerInfo.phantoms.containsKey(port)) {
                    isExisted = true;
                    break;
                }
                else {
                    ctx = entry.getKey();
                }
            }
        }

        if (isExisted) {
            result.addProperty("success", false);
            result.addProperty("message", "The phantom server is existed !");
        }
        else if (ctx == null) {
            result.addProperty("success", false);
            result.addProperty("message", "The worker is not available !");
        }
        else {
            result.addProperty("success", true);
            result.addProperty("message", "The phantom server will start in seconds !");
            ctx.writeAndFlush(Messages.Prt.NewPhantomServer.newBuilder().setPort(port).build());
        }
        return result;
    }

    public static class WorkerInfo {

        public final ChannelHandlerContext context;

        public final String host;

        public final int port;

        public final TreeMap<Integer, PhantomInfo> phantoms;

        public WorkerInfo(ChannelHandlerContext context) {
            this.context = context;
            InetSocketAddress address = (InetSocketAddress) context.channel().remoteAddress();
            this.host = address.getHostString();
            this.port = address.getPort();
            phantoms = new TreeMap<>(new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    return (o1 == null ? 0 : o1) - (o2 == null ? 0 : o2);
                }
            });
        }

        public void phantomStart(int port) {
            phantoms.put(port, new PhantomInfo(host, port));
        }

        public void phantomStop(int port) {
            phantoms.remove(port);
        }

        public static class PhantomInfo {

            public final String host;

            public final int port;

            public final static int maxTaskNum = 8;

            private AtomicInteger curTaskNum = new AtomicInteger(0);

            //phantom connection refused 的次数，在com/xiyuan/spider/task/DefaultTask.java:57处连接拒接时增加次数，如果正常连接则重置次数
            private int refusedNum = 0;

            //第一次拒绝访问的时间，在refusedNum > 0时候有效，恢复后重置为 0
            private long firstRefusedTime = 0;

            //拒绝访问后 8 秒钟重启 phantomServer
            private static final long timeToRestartAfterRefused = 8000;

            public int getTaskNum() {
                return curTaskNum.get() + refusedNum;
            }

            public PhantomInfo(String host, int port) {
                this.host = host;
                this.port = port;
            }

            public void increaseTask() {
                curTaskNum.incrementAndGet();
            }

            public void decreaseTask() {
                curTaskNum.decrementAndGet();
            }

            public void resetRefusedNum() {
                refusedNum = 0;
            }

            public void increaseRefusedNum() {
                refusedNum ++;
                if (firstRefusedTime == 0) {
                    firstRefusedTime = System.currentTimeMillis();
                }
            }

            public boolean isBusy() {
                return curTaskNum.get() + refusedNum >= maxTaskNum;
            }
        }

    }

}
