package com.xiyuan.spider.task;

import com.xiyuan.spider.annotation.OnTime;
import org.quartz.CronExpression;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Date;

/**
 * Created by xiyuan_fengyu on 2017/3/3.
 */
public class OnTimeTask extends DefaultTask {

    public final String url;

    private String cronStr;

    private CronExpression cron;

    private long nextExcuteTime;

    public String getCronStr() {
        return cronStr;
    }

    public long getNextExcuteTime() {
        return nextExcuteTime;
    }

    public OnTimeTask(OnTime onTime, Object callbackObject, Method callbackMethod) throws ParseException {
        super(onTime.name(), onTime.js(), onTime.timeout(), callbackObject, callbackMethod);
        url = onTime.url();
        cronStr = onTime.cron();
        cron = new CronExpression(cronStr);
        beforeExcute();
    }

    public boolean updateCron(String newCron) {
        if (CronExpression.isValidExpression(newCron)) {
            try {
                cron = new CronExpression(newCron);
                cronStr = newCron;
                beforeExcute();
                return true;
            }
            catch (Exception e) {
                return false;
            }
        }
        else {
            return false;
        }
    }

    @Override
    protected void beforeExcute() {
        nextExcuteTime = cron.getNextValidTimeAfter(new Date()).getTime();
    }
}
