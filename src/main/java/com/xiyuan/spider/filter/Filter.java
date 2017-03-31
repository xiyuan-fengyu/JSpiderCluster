package com.xiyuan.spider.filter;

import com.xiyuan.spider.message.Message;

import java.io.Serializable;

/**
 * Created by xiyuan_fengyu on 2017/2/21.
 */
public interface Filter extends Serializable {

    void setExisted(Message msg);

    boolean isExisted(Message msg);

    void clear();

}
