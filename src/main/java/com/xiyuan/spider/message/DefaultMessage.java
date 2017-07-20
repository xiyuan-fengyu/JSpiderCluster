package com.xiyuan.spider.message;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * Created by xiyuan_fengyu on 2017/2/17.
 */
public class DefaultMessage implements Message {

    private static final long serialVersionUID = -7928547442288463790L;

    private String key;

    private String url;

    private int priority;

    private static final Gson gson = new Gson();

    private HashMap<String, Object> userDatas = new HashMap<>();

    public DefaultMessage(String url) {
        this(url, url, 0);
    }

    public DefaultMessage(String url, String key, int priority) {
        this.url = url;
        this.key = key;
        this.priority = priority;
    }

    public <T> void addUserData(String key, T t) {
        if (key != null && t != null) {
            userDatas.put(key, t);
        }
    }

    public void setProxyIp(String ip) {
        addUserData("proxyIp", ip);
    }

    public void setProxyPort(int port) {
        addUserData("proxyPort", port);
    }

    public void setProxy(String proxy) {
        if (proxy != null) {
            try {
                String[] split = proxy.split(":");
                addUserData("proxyIp", split[0]);
                addUserData("proxyPort", Integer.parseInt(split[1]));
            }
            catch (Exception e) {
                System.err.println("代理格式有误：" + proxy);
            }
        }
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String url() {
        if (userDatas.isEmpty()) {
            return url;
        }

        try {
            return url + (url.matches(".*\\?.*?=.*?") ? "&" : "?") + "userDatas=" + URLEncoder.encode(gson.toJson(userDatas), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return url;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int p) {
        this.priority = p;
    }

    @Override
    public int compareTo(Message o) {
        if (o == null) {
            return -1;
        }

        return this.getPriority() - o.getPriority();
    }

}
