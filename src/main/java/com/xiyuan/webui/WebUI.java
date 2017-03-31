package com.xiyuan.webui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xiyuan.config.AppInfo;
import com.xiyuan.config.ClusterCfg;
import com.xiyuan.webui.http.HttpRequestHandler;
import com.xiyuan.webui.websocket.WebSocketHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by xiyuan_fengyu on 2017/3/9.
 */
public class WebUI {

    private static WebUI instance;

    public static void startWebUI() {
        if (instance == null) {
            instance = new WebUI(ClusterCfg.cluster_master_webui_port);
        }
    }

    public static void shutdown() {
        if (instance != null) {
            instance.shutdownThis();
            instance = null;
        }
    }

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    private ChannelFuture channelFuture;

    private WebUI(int port) {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new InitChannel());

            channelFuture = bootstrap.bind(port).sync();
            System.out.println("WebUI server(" + ClusterCfg.cluster_master_host + ":" + port + ") started");

            //添加定时任务，每秒向客户端推送集群的状态信息
            workerGroup.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    if (WebSocketHandler.hasClients()) {
                        JsonObject clusterInfo = AppInfo.clusterInfo();
                        WebSocketHandler.sendDataToWebSocketClients(clusterInfo);
                    }
                }
            }, 1, 1, TimeUnit.SECONDS);
        }
        catch (Exception e) {
            e.printStackTrace();
            shutdownThis();
        }
    }

    private void shutdownThis() {
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

    private class InitChannel extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast("http-codec", new HttpServerCodec());
            pipeline.addLast("aggregator", new HttpObjectAggregator(1024 * 1024 * 128));
            pipeline.addLast("http-chunked", new ChunkedWriteHandler());
            pipeline.addLast("handler", new ChannelHandler());
        }

    }

    private class ChannelHandler extends SimpleChannelInboundHandler<Object> {

        @Override
        protected void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof FullHttpRequest) {
                FullHttpRequest request = (FullHttpRequest) msg;
                CharSequence upgrade = request.headers().get("Upgrade");
                if (upgrade != null && "websocket".equals(upgrade.toString().toLowerCase())) {
                    WebSocketHandler.handshake(ctx, request);
                }
                else {
                    HttpRequestHandler.handle(ctx, request);
                }
            }
            else if (msg instanceof WebSocketFrame) {
                WebSocketHandler.handler(ctx, (WebSocketFrame) msg);
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }

    }

    public static void main(String[] args) throws InterruptedException {
        startWebUI();
        Thread.sleep(5000);
    }

}
