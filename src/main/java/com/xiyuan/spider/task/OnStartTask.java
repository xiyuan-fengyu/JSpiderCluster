package com.xiyuan.spider.task;

import com.xiyuan.spider.annotation.OnStart;

import java.lang.reflect.Method;

/**
 * Created by xiyuan_fengyu on 2017/3/3.
 */
public class OnStartTask extends DefaultTask {

    public final String url;

    private boolean isRunning;

    private boolean isExecuted;

    private boolean isSuccess;

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isExecuted() {
        return isExecuted;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public OnStartTask(OnStart onStart, Object callbackObject, Method callbackMethod) {
        super(onStart.name(), onStart.js(), onStart.timeout(), callbackObject, callbackMethod);
        url = onStart.url();
    }

    public void reset() {
        isRunning = false;
        isExecuted = false;
    }

    @Override
    protected void beforeExcute() {
        isRunning = true;
        isExecuted = false;
    }

    @Override
    protected void afterExcute(boolean success) {
        isRunning = false;
        isExecuted = true;
        isSuccess = success;
    }

}
