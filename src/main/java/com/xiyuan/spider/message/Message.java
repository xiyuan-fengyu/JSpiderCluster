package com.xiyuan.spider.message;

import java.io.Serializable;

/**
 * Created by xiyuan_fengyu on 2017/2/17.
 */
public interface Message extends Serializable, Comparable<Message> {

    String key();

    String url();

    int getPriority();

    void setPriority(int p);

}
