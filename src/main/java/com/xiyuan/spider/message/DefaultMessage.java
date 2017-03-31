package com.xiyuan.spider.message;

/**
 * Created by xiyuan_fengyu on 2017/2/17.
 */
public class DefaultMessage implements Message {

    private static final long serialVersionUID = -7928547442288463790L;

    private String url;

    private int priority;

    public DefaultMessage(String url) {
        this.url = url;
    }

    public DefaultMessage(String url, int priority) {
        this.url = url;
        this.priority = priority;
    }

    @Override
    public String url() {
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
