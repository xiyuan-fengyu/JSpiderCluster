package com.xiyuan.spider.queue;

import com.xiyuan.spider.filter.Filter;
import com.xiyuan.spider.message.Message;

import java.io.Serializable;

/**
 * Created by xiyuan_fengyu on 2017/2/17.
 */
public interface Queue extends Serializable {

    void push(Message t);

    Message pop();

    int size();

    boolean isEmpty();

    Filter getFilter(Class<? extends Filter> filterType);

    void setFilter(Class<? extends Filter> filterType, Filter filter);

}
