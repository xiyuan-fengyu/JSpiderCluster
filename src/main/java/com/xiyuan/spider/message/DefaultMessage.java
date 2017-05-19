package com.xiyuan.spider.message;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Created by xiyuan_fengyu on 2017/2/17.
 */
public class DefaultMessage implements Message {

    private static final long serialVersionUID = -7928547442288463790L;

    private String url;

    private int priority;

    private static final Gson gson = new Gson();

    private JsonObject userDatas = new JsonObject();

    public DefaultMessage(String url) {
        this.url = url;
    }

    public DefaultMessage(String url, int priority) {
        this.url = url;
        this.priority = priority;
    }

    public <T> void addUserData(String key, T t) {
        if (key != null && t != null) {
            userDatas.add(key, gson.toJsonTree(t));
        }
    }

    public void setProxyIp(String ip) {
        addUserData("proxyIp", ip);
    }

    public void setProxyPort(int port) {
        addUserData("proxyPort", port);
    }

    @Override
    public String url() {
        try {
            return url + (url.matches(".*\\?.*?=.*?") ? "&" : "?") + "userDatas=" + URLEncoder.encode(userDatas.toString(), "UTF-8");
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
