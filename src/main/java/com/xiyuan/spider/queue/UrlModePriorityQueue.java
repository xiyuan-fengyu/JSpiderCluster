package com.xiyuan.spider.queue;

import com.xiyuan.spider.message.Message;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by xiyuan_fengyu on 2017/2/28.
 */
public class UrlModePriorityQueue extends DefaultPriorityQueue {

    private static final long serialVersionUID = -7725931073968044893L;

    private HashMap<String, Integer> urlModeCounts = new HashMap<>();

    @Override
    protected void computePriority(Message msg) {
        String urlMode = urlMode(msg.url());
        int count = urlModeCounts.containsKey(urlMode) ? urlModeCounts.get(urlMode) + 1 : 1;
        urlModeCounts.put(urlMode, count);
        msg.setPriority(count);
    }

    private static String urlMode(String url) {
        if (url == null || url.isEmpty()) return "";

        int doltIndex = url.indexOf('.');
        int slashIndex = url.indexOf('/', doltIndex);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(url.substring(0, slashIndex + 1));

        String[] split = url.substring(slashIndex + 1).split("/");
        if (split.length >= 5) {
            stringBuilder.append("**/");
        }
        else {
            for (String str : split) {
                if (str.matches("[0-9]+")) {
                    stringBuilder.append("0/");
                }
                else if (str.length() > 0) {
                    stringBuilder.append("*/");
                }
            }
        }
        return stringBuilder.toString();
    }

}
