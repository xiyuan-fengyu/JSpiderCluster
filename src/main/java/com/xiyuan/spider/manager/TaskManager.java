package com.xiyuan.spider.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xiyuan.cluster.controller.MasterController;
import com.xiyuan.common.util.ClassUtil;
import com.xiyuan.common.util.DateUtil;
import com.xiyuan.spider.annotation.OnMessage;
import com.xiyuan.spider.annotation.OnStart;
import com.xiyuan.spider.annotation.OnTime;
import com.xiyuan.spider.annotation.Task;
import com.xiyuan.spider.message.Message;
import com.xiyuan.spider.phantom.PhantomServer;
import com.xiyuan.spider.queue.Queue;
import com.xiyuan.spider.task.DefaultTask;
import com.xiyuan.spider.task.OnMessageTask;
import com.xiyuan.spider.task.OnStartTask;
import com.xiyuan.spider.task.OnTimeTask;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by xiyuan_fengyu on 2017/3/3.
 */
public class TaskManager {

    private static final ExecutorService cachedThreadPool = Executors.newCachedThreadPool();

    private static boolean isAllOnStartTaskExecuted = false;

    private static final ArrayList<OnStartTask> onStartTasks = new ArrayList<>();

    private static final ArrayList<OnTimeTask> onTimeTasks = new ArrayList<>();

    private static final HashMap<String, OnMessageTask> onMessageTasks = new HashMap<>();

    public static JsonArray getOnStartTasks() {
        JsonArray arr = new JsonArray();
        for (OnStartTask task : onStartTasks) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", task.hashCode());
            obj.addProperty("name", task.name);
            obj.addProperty("url", task.url);
            obj.addProperty("js", task.js);
            obj.addProperty("state", task.isExecuted() ? "Executed" : (task.isRunning() ? "Running" : "Waiting"));
            obj.addProperty("success", task.isExecuted() ? (task.isSuccess() ? "true" : "false") : "");
            arr.add(obj);
        }
        return arr;
    }

    public static JsonArray getOnTimeTasks() {
        JsonArray arr = new JsonArray();
        for (OnTimeTask task : onTimeTasks) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", task.hashCode());
            obj.addProperty("name", task.name);
            obj.addProperty("url", task.url);
            obj.addProperty("js", task.js);
            obj.addProperty("cron", task.getCronStr());
            obj.addProperty("next", DateUtil.format(new Date(task.getNextExcuteTime())));
            arr.add(obj);
        }
        return arr;
    }

    public static JsonArray getOnMessageTasks() {
        JsonArray arr = new JsonArray();
        for (OnMessageTask task : onMessageTasks.values()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", task.hashCode());
            obj.addProperty("name", task.name);
            obj.addProperty("queue", task.fromQueue);
            obj.addProperty("js", task.js);
            obj.addProperty("parallel", task.getParallelMax());
            obj.addProperty("running", task.getParallelCur());
            if (QueueManager.queueMap.containsKey(task.fromQueue)) {
                obj.addProperty("num", QueueManager.queueMap.get(task.fromQueue).size());
            }
            else {
                obj.addProperty("num", 0);
            }
            arr.add(obj);
        }
        return arr;
    }

    public static void rerunOnStartTask(int hashCode) {
        for (OnStartTask task : onStartTasks) {
            if (task.hashCode() == hashCode) {
                isAllOnStartTaskExecuted = false;
                task.reset();
            }
        }
    }

    public static JsonObject updateOnTimeTask(int hashCode, String cron) {
        JsonObject result = new JsonObject();
        for (OnTimeTask task : onTimeTasks) {
            if (task.hashCode() == hashCode) {
                boolean flag = task.updateCron(cron);
                if (flag) {
                    sortOnTimeTasks();
                }
                result.addProperty("success", flag);
                result.addProperty("message", flag ? "OnTimeTask cron updated!" : "Wrong cron !");
                return result;
            }
        }
        result.addProperty("success", false);
        result.addProperty("message", "Task not found !");
        return result;
    }

    public static JsonObject updateOnMessageTask(int hashCode, String parallelStr) {
        JsonObject result = new JsonObject();
        try {
            int parallel = (int) Double.parseDouble(parallelStr);
            if (parallel < 0) parallel = 0;
            for (OnMessageTask task : onMessageTasks.values()) {
                if (task.hashCode() == hashCode) {
                    task.setParallelMax(parallel);
                    result.addProperty("success", true);
                    result.addProperty("message", "OnMessageTask parallel updated!");
                    return result;
                }
            }
            result.addProperty("success", false);
            result.addProperty("message", "Task not found !");
        }
        catch (Exception e) {
            result.addProperty("success", false);
            result.addProperty("message", "Please input a number !");
        }
        return result;
    }

    public static void updateOnMessageTask(String queueName, int parallel) {
        OnMessageTask task = onMessageTasks.get(queueName);
        if (task != null) {
            task.setParallelMax(parallel);
        }
    }

    public static void annotationScan() {
        annotationScan(null);
    }

    public static synchronized void annotationScan(Iterable<Class> classes) {
        isAllOnStartTaskExecuted = false;
        onStartTasks.clear();
        onTimeTasks.clear();
        onMessageTasks.clear();

        if (classes == null) {
            classes = ClassUtil.getClasses("", true);
        }

        OnStart onStartAnno;
        OnTime onTimeAnno;
        OnMessage onMessageAnno;
        for (Class clazz: classes) {
            if (clazz.getAnnotation(Task.class) != null) {
                try {
                    Object callbackObject = clazz.newInstance();
                    Method[] methods = clazz.getDeclaredMethods();
                    for (Method method : methods) {
                        boolean resetAccess = false;
                        if ((onStartAnno = method.getAnnotation(OnStart.class)) != null) {
                            onStartTasks.add(new OnStartTask(onStartAnno, callbackObject, method));
                            resetAccess = true;
                        }

                        if ((onTimeAnno = method.getAnnotation(OnTime.class)) != null) {
                            try {
                                onTimeTasks.add(new OnTimeTask(onTimeAnno, callbackObject, method));
                                resetAccess = true;
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        if ((onMessageAnno = method.getAnnotation(OnMessage.class)) != null) {
                            onMessageTasks.put(onMessageAnno.fromQueue(), new OnMessageTask(onMessageAnno, callbackObject, method));
                            resetAccess = true;
                        }

                        if (resetAccess && !method.isAccessible()) {
                            method.setAccessible(true);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        sortOnTimeTasks();
    }

    public static void dispatch() {
        if (cachedThreadPool.isShutdown()) {
            return;
        }

        //更新 onMessageTasks 的 parallel
        for (OnMessageTask task : onMessageTasks.values()) {
            task.updateParallelConfig();
        }

        List<MasterController.WorkerInfo.PhantomInfo> servers = MasterController.getPhantomServers();
        sortPhantomServers(servers);

        if (servers.isEmpty() || servers.get(0).isBusy()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (!isAllOnStartTaskExecuted) {
            for (OnStartTask task : onStartTasks) {
                if (servers.get(0).isBusy()) {
                    return;
                }
                else if (!task.isExecuted() && !task.isRunning()) {
                    task.excute(servers.get(0), task.url, cachedThreadPool);
                    sortPhantomServers(servers);
                }
            }
        }

        boolean shouldSort = false;
        for (int i =0, size = onTimeTasks.size(); i < size; i++) {
            if (servers.get(0).isBusy()) {
                return;
            }

            OnTimeTask onTimeTask = onTimeTasks.get(i);
            if (onTimeTask.getNextExcuteTime() <= now) {
                onTimeTask.excute(servers.get(0), onTimeTask.url, cachedThreadPool);
                sortPhantomServers(servers);
                shouldSort = true;
            }
            else {
                break;
            }
        }

        if (shouldSort) {
            sortOnTimeTasks();
        }

        if (servers.get(0).isBusy()) {
            return;
        }

        String queueName = nextQueueName();
        if (queueName != null) {
            Queue queue = QueueManager.queueMap.get(queueName);
            Message temp;
            if (onMessageTasks.containsKey(queueName)) {
                OnMessageTask task = onMessageTasks.get(queueName);
                while (!servers.get(0).isBusy() && !task.isBusy() && (temp = queue.pop()) != null) {
                    task.excute(servers.get(0), temp.url(), cachedThreadPool);
                    sortPhantomServers(servers);
                }
            }
        }
    }

    private static Iterator<String> it = QueueManager.queueMap.keySet().iterator();

    private static String nextQueueName() {
        synchronized (QueueManager.queueMap) {
            if (!it.hasNext()) {
                it = QueueManager.queueMap.keySet().iterator();
            }
            try {
                return it.hasNext() ? it.next() : null;
            }
            catch (Exception e) {
                it = QueueManager.queueMap.keySet().iterator();
                return null;
            }
        }
    }

    private static void sortPhantomServers(List<MasterController.WorkerInfo.PhantomInfo> servers) {
        if (servers.size() > 0) {
            Collections.sort(servers, new Comparator<MasterController.WorkerInfo.PhantomInfo>() {
                @Override
                public int compare(MasterController.WorkerInfo.PhantomInfo o1, MasterController.WorkerInfo.PhantomInfo o2) {
                    return (o1 == null ? 0 : o1.getTaskNum()) - (o2 == null ? 0: o2.getTaskNum());
                }
            });
        }
    }

    private static void sortOnTimeTasks() {
        Collections.sort(onTimeTasks, new Comparator<OnTimeTask>() {
            @Override
            public int compare(OnTimeTask o1, OnTimeTask o2) {
                return (int) ((o1 == null ? 0 : o1.getNextExcuteTime()) - (o2 == null ? 0 : o2.getNextExcuteTime()));
            }
        });
    }

    public static void shutdown() {
        cachedThreadPool.shutdown();
    }

    public static boolean isTerminated() {
        return cachedThreadPool.isTerminated();
    }

}
