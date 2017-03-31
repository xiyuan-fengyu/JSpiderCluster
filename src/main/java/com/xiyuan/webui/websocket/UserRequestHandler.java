package com.xiyuan.webui.websocket;

import com.google.gson.JsonObject;
import com.xiyuan.cluster.controller.MasterController;
import com.xiyuan.cluster.node.Master;
import com.xiyuan.luncher.MasterLuncher;
import com.xiyuan.spider.JSpiderMaster;
import com.xiyuan.spider.manager.QueueManager;
import com.xiyuan.spider.manager.TaskManager;
import com.xiyuan.webui.WebUI;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * Created by xiyuan_fengyu on 2017/3/10.
 */
public class UserRequestHandler {

    private static final HashMap<String, Method> methodCache = new HashMap<>();

    public static void dispatchRequest(ChannelHandlerContext ctx, JsonObject msg) {
        String key = msg.has("key") ? msg.get("key").getAsString() : "";
        Method method;
        if (methodCache.containsKey(key)) {
            method = methodCache.get(key);
        }
        else {
            try {
                method = UserRequestHandler.class.getDeclaredMethod(key, ChannelHandlerContext.class, JsonObject.class);
            } catch (NoSuchMethodException e) {
                method = null;
            }
        }

        if (method != null) {
            try {
                method.invoke(UserRequestHandler.class, ctx, msg);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    private static void getUrlFromQueue(ChannelHandlerContext ctx, JsonObject msg) {
        if (msg.has("id") && msg.has("value")) {
            JsonObject value = msg.get("value").getAsJsonObject();
            String queue = value.get("queue").getAsString();
            value.addProperty("url", QueueManager.getUrlFromQueue(queue));
            ctx.writeAndFlush(new TextWebSocketFrame(msg.toString()));
        }
    }

    private static void saveTaskChange(ChannelHandlerContext ctx, JsonObject msg) {
        if (msg.has("id") && msg.has("value")) {
            JsonObject value = msg.get("value").getAsJsonObject();
            int taskId = value.get("taskId").getAsInt();
            String taskType = value.get("taskType").getAsString();
            if ("onStartTask".equals(taskType)) {
                TaskManager.rerunOnStartTask(taskId);
                return;//不返回信息
            }
            else if ("onTimeTask".equals(taskType)) {
                String cron = value.get("newValue").getAsString();
                msg.add("value", TaskManager.updateOnTimeTask(taskId, cron));
            }
            else if ("onMessageTask".equals(taskType)) {
                try {
                    msg.add("value", TaskManager.updateOnMessageTask(taskId, value.get("newValue").getAsString()));
                }
                catch (Exception e) {
                    //
                }
            }
            else if ("newPhantom".equals(taskType)) {
                String host = value.get("host").getAsString();
                int port = -1;
                try {
                    port = value.get("newValue").getAsInt();
                }
                catch (Exception e) {
                    //
                }
                msg.add("value", MasterController.newPhantomServer(host.split(":")[0], port));
            }
            ctx.writeAndFlush(new TextWebSocketFrame(msg.toString()));
        }
    }

    private static void restartPhantom(ChannelHandlerContext ctx, JsonObject msg) {
        if (msg.has("value")) {
            JsonObject value = msg.get("value").getAsJsonObject();
            String host = value.get("worker").getAsString().split(":")[0];
            int port = value.get("port").getAsInt();
            MasterController.restartPhantomServer(host, port);
        }
    }

    private static void stopPhantom(ChannelHandlerContext ctx, JsonObject msg) {
        if (msg.has("value")) {
            JsonObject value = msg.get("value").getAsJsonObject();
            String host = value.get("worker").getAsString().split(":")[0];
            int port = value.get("port").getAsInt();
            MasterController.stopPhantomServer(host, port);
        }
    }

    private static void shutdownMaster(final ChannelHandlerContext ctx, JsonObject msg) {
        new Thread() {
            @Override
            public void run() {
                JsonObject res = new JsonObject();
                res.addProperty("key", "shutdownProgress");

                res.addProperty("message", "The master is closing!");
                res.addProperty("progress", 0);
                ctx.writeAndFlush(new TextWebSocketFrame(res.toString()));
                MasterLuncher.shutdown();
                res.addProperty("message", "The master has been colsed!");
                res.addProperty("progress", 25);
                ctx.writeAndFlush(new TextWebSocketFrame(res.toString()));

                long now = System.currentTimeMillis();
                while (!TaskManager.isTerminated()) {
                    res.addProperty("message", "The task manager is closing ... " + (System.currentTimeMillis() - now) / 1000 + " s");
                    res.addProperty("progress", 40);
                    ctx.writeAndFlush(new TextWebSocketFrame(res.toString()));
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                res.addProperty("message", "The task manager has been closed!");
                res.addProperty("progress", 50);
                ctx.writeAndFlush(new TextWebSocketFrame(res.toString()));

                res.addProperty("message", "Saving the message queues to disk!");
                res.addProperty("progress", 75);
                ctx.writeAndFlush(new TextWebSocketFrame(res.toString()));
                //存储
                QueueManager.saveQueueToDisk();
                res.addProperty("message", "The message queues hve been saved to disk!");
                res.addProperty("progress", 100);
                ctx.writeAndFlush(new TextWebSocketFrame(res.toString()));
            }
        }.start();
    }

    private static void shutdownWebUI(ChannelHandlerContext ctx, JsonObject msg) {
        WebUI.shutdown();
        MasterLuncher.exit();
    }

}
