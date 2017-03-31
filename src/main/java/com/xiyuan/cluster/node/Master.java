package com.xiyuan.cluster.node;

import com.xiyuan.cluster.controller.MasterController;
import com.xiyuan.cluster.decoder.PrtDecoder;
import com.xiyuan.cluster.encoder.PrtEncoder;
import com.xiyuan.cluster.msg.Messages;
import com.xiyuan.config.ClusterCfg;
import com.xiyuan.spider.manager.TaskManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Created by xiyuan_fengyu on 2017/3/1.
 */
public class Master {

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    private ChannelFuture channelFuture;

    public Master(int port) {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new MainHandler());

            channelFuture = bootstrap.bind(port).sync();
            System.out.println("master(" + ClusterCfg.cluster_master_host + ":" + port + ") started");

            workerGroup.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        TaskManager.dispatch();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 1000, 250, TimeUnit.MILLISECONDS);
        }
        catch (Exception e) {
            e.printStackTrace();
            shutdown();
        }
    }

    public void shutdown() {
        TaskManager.shutdown();

        if (channelFuture != null) {
            channelFuture.channel().close();
            channelFuture = null;
        }

        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            bossGroup = null;
        }

        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
    }

    private class MainHandler extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(new ProtobufVarint32FrameDecoder());
            pipeline.addLast(new ProtobufDecoder(Messages.Prt.getDefaultInstance()));
            pipeline.addLast(new PrtDecoder());
            pipeline.addLast(new ProtobufVarint32LengthFieldPrepender());
            pipeline.addLast(new ProtobufEncoder());
            pipeline.addLast(new PrtEncoder());
            pipeline.addLast(new MessageHandler());
        }

    }

    private class MessageHandler extends ChannelHandlerAdapter {

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
            String workerHost = address.getHostString();
            if (!ClusterCfg.cluster_workers.containsKey(workerHost)) {
                ctx.close();
            }
            else {
                System.out.println("worker(" + workerHost + ":" + address.getPort() + ") connected");
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            MasterController.channelActive(ctx);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
            System.out.println("worker(" + address.getHostString() + ":" + address.getPort() + ") disconnected");
            MasterController.channelUnregistered(ctx);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            MasterController.channelRead(ctx, msg);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.close();
        }
    }

}
