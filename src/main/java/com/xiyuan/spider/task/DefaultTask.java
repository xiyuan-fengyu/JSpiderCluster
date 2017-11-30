package com.xiyuan.spider.task;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xiyuan.cluster.controller.MasterController;
import com.xiyuan.common.log.LogManager;
import com.xiyuan.common.util.ClassUtil;
import com.xiyuan.common.util.GsonUtil;
import com.xiyuan.common.util.HttpUtil;
import com.xiyuan.config.AppInfo;
import com.xiyuan.spider.annotation.AddToQueue;
import com.xiyuan.spider.annotation.OnError;
import com.xiyuan.spider.manager.QueueManager;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by xiyuan_fengyu on 2017/3/3.
 */
public class DefaultTask {

    public final String name;

    public final String js;

    public final int timeout;

    public final Object callbackObject;

    public final Method callbackMethod;

    private static Logger logger = LogManager.logger(DefaultTask.class);

    public DefaultTask(String name, String js, int timeout, Object callbackObject, Method callbackMethod) {
        this.name = name;
        this.js = js;
        this.timeout = timeout;
        this.callbackObject = callbackObject;
        this.callbackMethod = callbackMethod;
    }

    protected void beforeExcute() {}

    public synchronized void excute(final MasterController.WorkerInfo.PhantomInfo server, final String url, final int depth, ExecutorService cachedThreadPool) {
        beforeExcute();
        AppInfo.deltaRunningTaskNum(1);
        server.increaseTask();

        cachedThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                boolean success = false;
                try {
                    String responseStr = null;
                    if ("".equals(url) || "".equals(js)) {
                        responseStr = "{\"return\": \"\"}";
                    }
                    else {
                        String reqUrl = "http://" + server.host + ":" + server.port + "/crawl?js=" + js + "&timeout=" + timeout + "&url=" + url;
                        logger.debug("Request to phantom: " + reqUrl);
                        try {
                            responseStr = HttpUtil.get(reqUrl);
                            server.resetRefusedNum();
                        }
                        catch (Exception e) {
                            logger.warn("Phantom refused: " + reqUrl);
                            responseStr = "{\"error\": \"PhantomRefused\"}";
                            server.increaseRefusedNum();
                        }
                    }

                    JsonObject json = GsonUtil.jsonParser.parse(responseStr).getAsJsonObject();
                    Object res = null;
                    Method callM = null;
                    if (json.has("return")) {
                        success = true;
                        AppInfo.deltaSuccessTaskNum(1);

                        try {
                            Object resultObj = ClassUtil.jsonEleTranslate(json.get("return"), callbackMethod.getParameterTypes()[1]);
                            if (callbackMethod.getParameterTypes().length == 2) {
                                res = callbackMethod.invoke(callbackObject, url, resultObj);
                            }
                            else {
                                JsonObject status = json.get("status").getAsJsonObject();
                                status.addProperty("depth", depth);
                                res = callbackMethod.invoke(callbackObject, url, resultObj, status);
                            }
                        }
                        catch (Exception e) {
                            res = null;
                            logger.error("callback method (" + callbackMethod.getName() + ") invoked with error", e);
                        }

                        callM = callbackMethod;
                    }
                    else if (json.has("error")) {
                        AppInfo.deltaFailTaskNum(1);

                        try {
                            OnError onError = callbackMethod.getAnnotation(OnError.class);
                            if (onError != null) {
                                callM = callbackObject.getClass().getMethod(onError.callback(), String.class, JsonObject.class);
                            }
                        } catch (NoSuchMethodException e) {
//                                e.printStackTrace();
                        }

                        if (callM == null) {
                            try {
                                callM = callbackObject.getClass().getMethod("onError", String.class, JsonObject.class);
                            } catch (NoSuchMethodException e1) {
//                                    e1.printStackTrace();
                            }
                        }

                        boolean errorDealed = false;
                        if (callM != null) {
                            try {
                                res = callM.invoke(callbackObject, url, json);
                                errorDealed = true;
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        if (!errorDealed) {
                            logger.error("\nTask executed with error:\nurl:    " + url + "\n" + json);
                        }
                    }

                    if (res != null) {
                        AddToQueue addToQueue = callM.getAnnotation(AddToQueue.class);
                        if (addToQueue != null) {
                            QueueManager.addToQueue(res, addToQueue, depth + 1);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                afterExcute(success);
                AppInfo.deltaRunningTaskNum(-1);
                server.decreaseTask();
            }
        });
    }

    protected void afterExcute(boolean success) {}

}
