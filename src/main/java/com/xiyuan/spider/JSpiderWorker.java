package com.xiyuan.spider;

import com.google.protobuf.ByteString;
import com.xiyuan.cluster.msg.Messages;
import com.xiyuan.common.util.FileUtil;
import com.xiyuan.common.watcher.FileWatchers;
import com.xiyuan.config.AppInfo;
import com.xiyuan.config.ClusterCfg;
import com.xiyuan.spider.phantom.PhantomServer;
import io.netty.channel.ChannelHandlerContext;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.HashMap;

/**
 * Created by xiyuan_fengyu on 2017/3/3.
 */
public class JSpiderWorker {

    private static HashMap<Integer, PhantomServer> phantomServers = new HashMap<>();

    public static void startPhantoms(final ChannelHandlerContext ctx, ClusterCfg.WorkerCfg workerCfg) {
        for (int port : workerCfg.phantom_ports) {
            startPhantom(ctx, port);
        }
    }


    public static void startPhantom(final ChannelHandlerContext ctx, final int port) {
        PhantomServer phantomServer = new PhantomServer(port, new PhantomServer.PhantomServerListener() {
            @Override
            public void onStart(PhantomServer server) {
                ctx.writeAndFlush(Messages.Prt.PhantomStart.newBuilder().setPort(server.port).build());
                phantomServers.put(server.port, server);
            }

            @Override
            public void onStop(PhantomServer server) {
                ctx.writeAndFlush(Messages.Prt.PhantomEnd.newBuilder().setPort(server.port).build());
                phantomServers.remove(server.port);
            }
        });
    }

    public static void stopPhantoms(ClusterCfg.WorkerCfg workerCfg) {
        for (PhantomServer server : phantomServers.values()) {
            server.destory();
        }
    }

    public static void stopPhantom(int port) {
        if (phantomServers.containsKey(port)) {
            phantomServers.get(port).destory();
        }
    }

    public static void restartPhantom(int port) {
        if (phantomServers.containsKey(port)) {
            phantomServers.get(port).restart();
        }
    }

    public static void restartAllPhantom() {
        for (PhantomServer server : phantomServers.values()) {
            server.restart();
        }
    }

    /**
     * 目前主要用于截图 传回 master
     * @param ctx 通道上下文
     */
    public static void startFileWatcher(final ChannelHandlerContext ctx) {
        final boolean atSameMachine = ((InetSocketAddress) ctx.channel().remoteAddress()).getHostString().equals(((InetSocketAddress) ctx.channel().localAddress()).getHostString());
        FileWatchers.add(new FileWatchers.FileListener() {
            @Override
            public void onChange(File file) {
                if (!atSameMachine) {
                    if (file.exists()) {
                        String relativePath = file.getAbsolutePath().substring((AppInfo.getScreenshotPath()).length() + 1);
                        ctx.writeAndFlush(Messages.Prt.ScreenshotFile.newBuilder().addShareFiles(
                                Messages.Prt.ShareFile.newBuilder().setRelativePath(relativePath).setContentBytes(ByteString.copyFrom(FileUtil.bytes(file)))
                        ).build());
                        file.delete();
                    }
                }
            }
        }, AppInfo.getScreenshotPath(), null);

        FileWatchers.add(new FileWatchers.FileListener() {
            @Override
            public void onChange(File file) {
                if (!atSameMachine) {
                    if (file.exists()) {
                        String relativePath = file.getAbsolutePath().substring((AppInfo.getDownloadPath()).length() + 1);
                        ctx.writeAndFlush(Messages.Prt.DownloadFile.newBuilder().addShareFiles(
                                Messages.Prt.ShareFile.newBuilder().setRelativePath(relativePath).setContentBytes(ByteString.copyFrom(FileUtil.bytes(file)))
                        ).build());
                        file.delete();
                    }
                }
            }
        }, AppInfo.getDownloadPath(), null);
    }

}
