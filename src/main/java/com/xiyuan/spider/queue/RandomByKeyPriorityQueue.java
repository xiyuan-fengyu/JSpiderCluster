package com.xiyuan.spider.queue;

import com.xiyuan.spider.message.Message;

/**
 * Created by xiyuan_fengyu on 2017/2/28.
 */
public class RandomByKeyPriorityQueue extends DefaultPriorityQueue {

    private static final long serialVersionUID = -7547015624262181312L;

    @Override
    protected void computePriority(Message msg) {
        msg.setPriority((int) (Math.random() * msg.key().length() * 100));
    }

}
