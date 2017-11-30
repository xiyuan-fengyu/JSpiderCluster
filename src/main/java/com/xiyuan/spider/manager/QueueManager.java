package com.xiyuan.spider.manager;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xiyuan.common.util.GsonUtil;
import com.xiyuan.common.util.ObjectCacheUtil;
import com.xiyuan.config.AppInfo;
import com.xiyuan.spider.annotation.AddToQueue;
import com.xiyuan.spider.filter.Filter;
import com.xiyuan.spider.message.DefaultMessage;
import com.xiyuan.spider.message.Message;
import com.xiyuan.spider.queue.Queue;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by xiyuan_fengyu on 2017/2/17.
 */
public class QueueManager {

    static final HashMap<String, Queue> queueMap;

    //load from disk cache
    static {
        HashMap<String, Queue> cache = null;
        try {
            //noinspection unchecked
            cache = (HashMap<String, Queue>) ObjectCacheUtil.load(AppInfo.getCachePath() + "/queueMap.cache");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if (cache != null) {
            queueMap = cache;
        }
        else {
            queueMap = new HashMap<>();
        }
    }

    public static void saveQueueToDisk() {
        ObjectCacheUtil.save(queueMap, AppInfo.getCachePath() + "/queueMap.cache");
    }

    public static String getUrlFromQueue(String queue) {
        if (queueMap.containsKey(queue)) {
            Message message = queueMap.get(queue).pop();
            return message == null ? "" : message.url();
        }
        return "";
    }

    private static void addQueue(String name, Queue queue) {
        synchronized (queueMap) {
            queueMap.put(name, queue);
        }
    }

    public static void addToQueue(Object datas, final String queueName, final Class<? extends Queue> queueType, final Class<? extends Filter> filterType) {
        addToQueue(datas, queueName, queueType, filterType, 0);
    }

    public static void addToQueue(Object datas, final String queueName, final Class<? extends Queue> queueType, final Class<? extends Filter> filterType, int depth) {
        if (datas != null) {
            Queue queue = queueMap.get(queueName);
            if (queue == null) {
                try {
                    queue = queueType.newInstance();
                    addQueue(queueName, queue);
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

            if (queue != null) {
                if (filterType != null && queue.getFilter(filterType) == null) {
                    try {
                        queue.setFilter(filterType, filterType.newInstance());
                    } catch (InstantiationException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }

                Filter filter = queue.getFilter(filterType);

                if (datas.getClass().toString().startsWith("class [L")) {
                    Object[] objs = (Object[]) datas;
                    for (Object obj: objs) {
                        tranAndAdd(queue, obj, filter, depth);
                    }
                }
                else if (datas instanceof Iterable) {
                    Iterator it = ((Iterable) datas).iterator();
                    while (it.hasNext()) {
                        Object obj = it.next();
                        tranAndAdd(queue, obj, filter, depth);
                    }
                }
                else {
                    tranAndAdd(queue, datas, filter, depth);
                }
            }
        }
    }

    public static void addToQueue(Object datas, AddToQueue anno) {
        addToQueue(datas, anno, 0);
    }

    public static void addToQueue(Object datas, AddToQueue anno, int depth) {
        if (anno != null) {
            addToQueue(datas, anno.name(), anno.type(), anno.filter());
        }
    }

    private static void tranAndAdd(Queue queue, Object obj, Filter filter, int depth) {
        Message msg = null;
        if (obj instanceof Message) {
            msg = (Message) obj;
        }
        else if (obj instanceof String) {
            msg = new DefaultMessage((String) obj);
        }
        else if (obj instanceof JsonElement) {
            msg = new DefaultMessage(((JsonElement) obj).getAsString());
        }

        if (msg == null) {
            return;
        }

        if (msg.getDepth() < depth) {
            msg.setDepth(depth);
        }

        if (filter == null) {
            queue.push(msg);
        }
        else {
            synchronized (filter) {
                if (!filter.isExisted(msg)) {
                    queue.push(msg);
                    filter.setExisted(msg);
                }
            }
        }
    }

    public static void previewQueues() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\nnum of queues: ").append(queueMap.size()).append("\n");
        for (Map.Entry<String, Queue> keyVal: queueMap.entrySet()) {
            String name = keyVal.getKey();
            Queue queue = keyVal.getValue();
            stringBuilder.append(name).append(" -> ").append(queue.size()).append("\n");
        }
        stringBuilder.append("\n");
        System.out.println(stringBuilder.toString());
    }

}
