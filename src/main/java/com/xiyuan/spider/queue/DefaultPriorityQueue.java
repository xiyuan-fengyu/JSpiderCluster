package com.xiyuan.spider.queue;

import com.xiyuan.spider.message.Message;

import java.io.Serializable;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Created by xiyuan_fengyu on 2017/2/28.
 */
public abstract class DefaultPriorityQueue extends AbsQueue {

    private static final long serialVersionUID = 8107664626880918352L;

    private PriorityBlockingQueue<Message> queue = new PriorityBlockingQueue<>();

    abstract void computePriority(Message msg);

    @Override
    public void push(Message t) {
        computePriority(t);
        queue.add(t);
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
