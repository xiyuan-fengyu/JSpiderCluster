package com.xiyuan.spider.queue;

import com.xiyuan.spider.filter.Filter;
import com.xiyuan.spider.message.Message;

import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by xiyuan_fengyu on 2017/2/17.
 */
public class DefaultQueue extends AbsQueue {

    private static final long serialVersionUID = 4567819615365537769L;

    private ConcurrentLinkedQueue<Message> queue = new ConcurrentLinkedQueue<>();

    @Override
    public void push(Message s) {
        queue.add(s);
    }

    @Override
    public Message pop() {
        return queue.poll();
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

}
