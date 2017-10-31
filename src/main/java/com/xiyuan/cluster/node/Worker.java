package com.xiyuan.cluster.node;

import com.xiyuan.cluster.controller.WorkerController;
import com.xiyuan.cluster.decoder.PrtDecoder;
import com.xiyuan.cluster.encoder.PrtEncoder;
import com.xiyuan.cluster.msg.Messages;
import com.xiyuan.config.ClusterCfg;
import com.xiyuan.spider.manager.TaskManager;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import java.util.concurrent.TimeUnit;

/**
 * Created by xiyuan_fengyu on 2017/3/1.
 */
public class Worker {

    private WorkerController controller;

    public Worker(ClusterCfg.WorkerCfg workerCfg) {
        controller = new WorkerController(workerCfg);

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ConnectionHandler());

            //周期性向该节点下的所有 phantomjs 进程发送 ping 信息，超过一定时间phantomjs未收到消息则自动停止
            group.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    controller.ping();
                }
            }, 2000, 4000, TimeUnit.MILLISECONDS);

            ChannelFuture future = bootstrap.connect(ClusterCfg.cluster_master_host, ClusterCfg.cluster_master_netty_port).sync();
            future.channel().closeFuture().sync();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            group.shutdownGracefully();
        }
    }

    private class ConnectionHandler extends ChannelInitializer<SocketChannel> {

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
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            controller.channelActive(ctx);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            controller.channelRead(ctx, msg);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            ctx.flush();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            if (controller != null) {
                controller.destory();
                controller = null;
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            if (controller != null) {
                controller.destory();
                controller = null;
            }
            ctx.close();
        }
    }

}
