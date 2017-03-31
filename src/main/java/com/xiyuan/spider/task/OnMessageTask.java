package com.xiyuan.spider.task;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xiyuan.common.util.GsonUtil;
import com.xiyuan.spider.annotation.OnMessage;
import org.quartz.CronExpression;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by xiyuan_fengyu on 2017/3/3.
 */
public class OnMessageTask extends DefaultTask {

    public final String fromQueue;

    private int parallelMax;

    private final ArrayList<CronParallel> parallelConfig;

    private AtomicInteger parallelCur = new AtomicInteger(0);

    public int getParallelCur() {
        return parallelCur.get();
    }

    public int getParallelMax() {
        return parallelMax;
    }

    public void setParallelMax(int parallelMax) {
        this.parallelMax = parallelMax;
    }

    public boolean isBusy() {
        return parallelCur.get() >= parallelMax;
    }

    public OnMessageTask(OnMessage onMessage, Object callbackObject, Method callbackMethod) {
        super(onMessage.name(), onMessage.js(), onMessage.timeout(), callbackObject, callbackMethod);
        this.fromQueue = onMessage.fromQueue();
        this.parallelMax = onMessage.parallel();

        ArrayList<CronParallel> tempParallelConfig = new ArrayList<>();
        if (!onMessage.parallelConfig().equals("")) {
            try {
                JsonObject config = GsonUtil.jsonParser.parse(onMessage.parallelConfig()).getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : config.entrySet()) {
                    String cron = entry.getKey();
                    if (CronExpression.isValidExpression(cron)) {
                        try {
                              tempParallelConfig.add(new CronParallel(new CronExpression(cron), entry.getValue().getAsInt()));
                        }
                        catch (Exception ee) {
                            //
                        }
                    }
                }
            }
            catch (Exception e) {
                //
            }
        }
        if (tempParallelConfig.size() > 0) {
            this.parallelConfig = tempParallelConfig;
            sortParallelConfig();
        }
        else {
            this.parallelConfig = null;
        }

    }

    private void sortParallelConfig() {
        if (this.parallelConfig != null) {
            Collections.sort(this.parallelConfig);
        }
    }

    public void updateParallelConfig() {
        if (parallelConfig != null) {
            boolean resortRequired = false;
            long now = System.currentTimeMillis();
            int minParallel = Integer.MAX_VALUE;
            for (int i = 0, size = parallelConfig.size(); i < size; i++) {
                CronParallel item = parallelConfig.get(i);
                if (item.nextTime <= now) {
                    resortRequired = true;
                    item.updateNextTime();
                    minParallel = Math.min(minParallel, item.parallel);
                }
                else {
                    break;
                }
            }

            if (resortRequired) {
                sortParallelConfig();
                this.parallelMax = minParallel;
            }
        }
    }

    @Override
    public void beforeExcute() {
        parallelCur.incrementAndGet();
    }

    @Override
    public void afterExcute(boolean success) {
        parallelCur.decrementAndGet();
    }

    private class CronParallel implements Comparable<CronParallel> {

        private final CronExpression cron;

        private long nextTime;

        private int parallel;

        private CronParallel(CronExpression cron, int parallel) {
            this.cron = cron;
            this.parallel = parallel;
            updateNextTime();
        }

        private void updateNextTime() {
            this.nextTime = cron.getNextValidTimeAfter(new Date()).getTime();
        }

        @Override
        public int compareTo(CronParallel o) {
            return (int) (this.nextTime - o.nextTime);
        }

    }

}
