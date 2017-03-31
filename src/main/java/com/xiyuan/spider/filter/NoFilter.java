package com.xiyuan.spider.filter;

import com.xiyuan.spider.message.Message;

/**
 * Created by xiyuan_fengyu on 2017/2/21.
 */
public class NoFilter implements Filter {

    private static final long serialVersionUID = -144141064053475426L;

    @Override
    public void setExisted(Message msg) {
    }

    @Override
    public boolean isExisted(Message msg) {
        return false;
    }

    @Override
    public void clear() {
    }
}
